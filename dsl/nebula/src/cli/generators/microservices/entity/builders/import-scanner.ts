import { Entity } from "../../../../../language/generated/ast.js";
import { EntityExt } from "../../../../types/ast-extensions.js";
import { TypeResolver } from "../../../common/resolvers/type-resolver.js";
import { getGlobalConfig } from "../../../common/config.js";
import { getEffectiveProperties, getEffectiveFieldMappings } from "../../../../utils/aggregate-helpers.js";
import { ImportManager } from "../../../../utils/import-manager.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../../services/dto-schema-service.js";

/**
 * Handles import detection and resolution for entity code generation.
 *
 * Responsibilities:
 * - Scans generated Java code for required imports
 * - Resolves DTO import paths
 * - Handles enum type imports
 * - Manages JPA annotation imports
 * - Generates DTO setter code with proper type mappings
 */
export class ImportScanner {
    constructor(
        private importManager: ImportManager,
        private dtoRegistry: DtoSchemaRegistry
    ) {}

    /**
     * Finalizes the Java code by scanning for imports and replacing the placeholder
     */
    finalizeWithImports(
        javaCode: string,
        projectName: string,
        isRootEntity: boolean,
        aggregateName: string,
        entityName: string,
        entity: EntityExt
    ): string {
        const detectedImports = this.scanCodeForImports(javaCode, projectName, isRootEntity, aggregateName, entityName, entity);

        this.importManager.clear();
        detectedImports.forEach(imp => {
            this.importManager.addCustomImport(imp);
        });

        this.addRequiredImports(isRootEntity, aggregateName);

        const formattedImports = this.importManager.formatImports();
        const importsString = formattedImports.join('\n');

        return javaCode.replace('IMPORTS_PLACEHOLDER', importsString);
    }

    /**
     * Scans generated code for imports needed based on annotations, types, and references
     */
    scanCodeForImports(
        javaCode: string,
        projectName: string,
        isRoot: boolean,
        aggregateName?: string,
        entityName?: string,
        entity?: EntityExt
    ): string[] {
        const imports: string[] = [];
        const dtoRegistry = this.dtoRegistry;

        // JPA annotations
        if (javaCode.includes('@Entity')) imports.push('import jakarta.persistence.Entity;');
        if (javaCode.includes('@Id')) imports.push('import jakarta.persistence.Id;');
        if (javaCode.includes('@GeneratedValue')) imports.push('import jakarta.persistence.GeneratedValue;');
        if (javaCode.includes('@OneToOne')) imports.push('import jakarta.persistence.OneToOne;');
        if (javaCode.includes('@OneToMany')) imports.push('import jakarta.persistence.OneToMany;');
        if (javaCode.includes('CascadeType')) imports.push('import jakarta.persistence.CascadeType;');
        if (javaCode.includes('FetchType')) imports.push('import jakarta.persistence.FetchType;');
        if (javaCode.includes('@Enumerated')) imports.push('import jakarta.persistence.Enumerated;');
        if (javaCode.includes('EnumType')) imports.push('import jakarta.persistence.EnumType;');

        // Java standard library
        if (javaCode.includes('LocalDateTime')) imports.push('import java.time.LocalDateTime;');
        if (javaCode.includes('BigDecimal')) imports.push('import java.math.BigDecimal;');
        if (javaCode.includes('Set<') || javaCode.includes('HashSet')) {
            imports.push('import java.util.Set;');
            imports.push('import java.util.HashSet;');
        }
        if (javaCode.includes('List<') || javaCode.includes('ArrayList')) {
            imports.push('import java.util.List;');
            imports.push('import java.util.ArrayList;');
        }
        if (javaCode.includes('Collectors')) imports.push('import java.util.stream.Collectors;');

        const config = getGlobalConfig();

        // Framework imports
        if (javaCode.includes('AggregateState')) {
            const aggregateStateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate.AggregateState;`;
            imports.push(aggregateStateImport);
        }

        if (isRoot) {
            const aggregateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate;`;
            imports.push(aggregateImport);
            // Root entities need EventSubscription for getEventSubscriptions() method
            const eventSubscriptionImport = `import ${config.getBasePackage()}.ms.domain.event.EventSubscription;`;
            imports.push(eventSubscriptionImport);

            // Add imports for subscription classes used in getEventSubscriptions()
            const subscriptionPattern = /new\s+([A-Z][a-zA-Z]*Subscribes[A-Z][a-zA-Z]*)\(/g;
            let subscriptionMatch;
            while ((subscriptionMatch = subscriptionPattern.exec(javaCode)) !== null) {
                const subscriptionClassName = subscriptionMatch[1];
                const subscriptionImport = `import ${config.buildPackageName(projectName, 'microservices', aggregateName?.toLowerCase() || 'unknown', 'events', 'subscribe')}.${subscriptionClassName};`;
                if (!imports.includes(subscriptionImport)) {
                    imports.push(subscriptionImport);
                }
            }

            // Add imports for invariants if SimulatorException is used
            if (javaCode.includes('SimulatorException')) {
                imports.push(`import ${config.getBasePackage()}.ms.exception.SimulatorException;`);
            }
            if (javaCode.includes('INVARIANT_BREAK')) {
                imports.push(`import static ${config.getBasePackage()}.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;`);
            }
        }

        // DTO imports
        const dtoPattern = /\b([A-Z][a-zA-Z]*Dto)\b/g;
        let dtoMatch;
        while ((dtoMatch = dtoPattern.exec(javaCode)) !== null) {
            const dtoType = dtoMatch[1];
            const importPath = this.resolveDtoImportPath(dtoType, projectName, aggregateName, dtoRegistry);
            if (importPath) {
                imports.push(importPath);
            }
        }

        // Entity imports
        const entityPattern = /\b([A-Z][a-zA-Z]*(?:User|Course|Question|Quiz|Topic|Tournament|Answer|Execution|Option))\b/g;
        let entityMatch;
        const excludedEntityNames = ['String', 'Integer', 'Long', 'Double', 'Float', 'Boolean', 'LocalDateTime', 'BigDecimal'];

        const samePackageEntityNames = new Set<string>();
        if (entity && entity.$container) {
            const aggregate = entity.$container as any;
            if (aggregate.entities && Array.isArray(aggregate.entities)) {
                aggregate.entities.forEach((e: Entity) => {
                    if (e.name) {
                        samePackageEntityNames.add(e.name);
                    }
                });
            }
        }

        while ((entityMatch = entityPattern.exec(javaCode)) !== null) {
            const entityType = entityMatch[1];
            if (!excludedEntityNames.includes(entityType) && !entityType.endsWith('Dto')) {
                if (!samePackageEntityNames.has(entityType) && entityType !== entityName && entityType !== aggregateName) {
                    const entityImport = `import ${config.buildPackageName(projectName, 'microservices', aggregateName?.toLowerCase() || 'unknown', 'aggregate')}.${entityType};`;
                    imports.push(entityImport);
                }
            }
        }

        // Enum imports from entity properties
        if (entity) {
            const enumTypes = new Set<string>();

            for (const prop of entity.properties) {
                if (prop.type && typeof prop.type === 'object' && prop.type.$type === 'EntityType' && prop.type.type) {
                    const ref = prop.type.type.ref;
                    if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                        const enumName = (ref as any).name;
                        if (enumName) {
                            enumTypes.add(enumName);
                        }
                    } else if (prop.type.type.$refText) {
                        const refText = prop.type.type.$refText;
                        const javaType = TypeResolver.resolveJavaType(prop.type);
                        if (!TypeResolver.isPrimitiveType(javaType) &&
                            !TypeResolver.isEntityType(javaType) &&
                            !javaType.startsWith('List<') &&
                            !javaType.startsWith('Set<')) {
                            enumTypes.add(refText);
                        }
                    }
                }
            }

            const excludedEnums = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState'];
            for (const enumType of enumTypes) {
                if (!excludedEnums.includes(enumType)) {
                    const enumImport = `import ${config.buildPackageName(projectName, 'shared', 'enums')}.${enumType};`;
                    if (!imports.includes(enumImport)) {
                        imports.push(enumImport);
                    }
                }
            }
        }

        // Enum imports from code patterns
        const enumPattern = /\b([A-Z][a-zA-Z]*(?:Type|Role|State))\b/g;
        let enumMatch;
        const excludedEnums = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState', 'LocalDateTime'];
        const foundEnums = new Set<string>();

        while ((enumMatch = enumPattern.exec(javaCode)) !== null) {
            const enumType = enumMatch[1];
            if (!excludedEnums.includes(enumType) &&
                !enumType.endsWith('Dto') &&
                !enumType.includes('List') &&
                !enumType.includes('Set')) {
                foundEnums.add(enumType);
            }
        }

        for (const enumType of foundEnums) {
            const enumImport = `import ${config.buildPackageName(projectName, 'shared', 'enums')}.${enumType};`;
            if (!imports.includes(enumImport)) {
                imports.push(enumImport);
            }
        }

        return imports.flat();
    }

    /**
     * Builds a DTO setter statement from schema information
     */
    buildDtoSetterFromSchema(
        field: DtoFieldSchema,
        entity: EntityExt,
        dtoFieldOverrides?: Map<string, { property: any; extractField?: string }>
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const override = dtoFieldOverrides?.get(field.name);
        const isRootEntity = entity.isRoot || false;

        // Handle aggregate fields
        if (field.isAggregateField && !override) {
            if (!isRootEntity) {
                return null;
            }
            switch (field.name) {
                case 'aggregateId':
                    return '        dto.setAggregateId(getAggregateId());';
                case 'version':
                    return '        dto.setVersion(getVersion());';
                case 'state':
                    return '        dto.setState(getState());';
                default:
                    return null;
            }
        }

        const effectiveProps = getEffectiveProperties(entity);
        const prop = override?.property || effectiveProps.find((p: any) => p.name === (field.sourceName || field.name));
        if (!prop) {
            return null;
        }
        const belongsToEntity = prop.$container?.name === entity.name || (prop as any).$fromMapping;
        if (!belongsToEntity && !override) {
            return null;
        }

        const getterCall = this.buildEntityGetterCall(prop);
        if (!getterCall) {
            return null;
        }

        const effectiveExtractField = override?.extractField || field.extractField;

        // Handle field mapping overrides
        if (override) {
            if (this.isEnumProperty(prop)) {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
            }
            if (effectiveExtractField) {
                const extractMethod = `get${effectiveExtractField.charAt(0).toUpperCase() + effectiveExtractField.slice(1)}()`;
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isEntityCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');
                const isDtoCollection = field.isCollection;

                if (isEntityCollection) {
                    const elementTypeMatch = javaType.match(/<(.*)>/);
                    if (elementTypeMatch) {
                        const elementType = elementTypeMatch[1];
                        if (TypeResolver.isPrimitiveType(elementType) ||
                            elementType === 'Integer' ||
                            elementType === 'String' ||
                            elementType === 'Boolean' ||
                            elementType === 'Long') {
                            return null;
                        }
                    }
                }

                if (isEntityCollection && isDtoCollection) {
                    const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                    let elementType = 'item';
                    if (javaType) {
                        const elementTypeMatch = javaType.match(/<(.*)>/);
                        if (elementTypeMatch) {
                            elementType = elementTypeMatch[1];
                        }
                    }
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map((${elementType} item) -> item.${extractMethod}).collect(${collector}) : null);`;
                } else {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${extractMethod} : null);`;
                }
            }
            return `        dto.set${capName}(${getterCall});`;
        }

        // Handle derived aggregate ID fields
        if (field.derivedAggregateId && field.sourceProperty) {
            const accessor = field.derivedAccessor || 'getAggregateId';
            if (field.isCollection) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(item -> item.${accessor}()).collect(${collector}) : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${accessor}() : null);`;
        }

        // Handle enum fields - convert to string using .name()
        if (field.isEnum || (prop && this.isEnumProperty(prop))) {
            if (field.isCollection) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(value -> value != null ? value.name() : null).collect(${collector}) : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
        }

        // Handle fields requiring DTO conversion
        if (field.requiresConversion) {
            if (field.isCollection && field.referencedEntityName) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedEntityName}::buildDto).collect(${collector}) : null);`;
                } else {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedDtoName}::new).collect(${collector}) : null);`;
                }
            }
            if (!field.isCollection) {
                if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.buildDto() : null);`;
                } else {
                    return `        dto.set${capName}(${getterCall} != null ? new ${field.referencedDtoName}(${getterCall}) : null);`;
                }
            }
        }

        // Legacy: Convert Type fields to string
        if (prop.name.endsWith('Type')) {
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.toString() : null);`;
        }

        // Handle extract field for other cases
        if (effectiveExtractField) {
            const extractMethod = `get${effectiveExtractField.charAt(0).toUpperCase() + effectiveExtractField.slice(1)}()`;
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isEntityCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');
            const isDtoCollection = field.isCollection;

            if (isEntityCollection) {
                const elementTypeMatch = javaType.match(/<(.*)>/);
                if (elementTypeMatch) {
                    const elementType = elementTypeMatch[1];
                    if (TypeResolver.isPrimitiveType(elementType) ||
                        elementType === 'Integer' ||
                        elementType === 'String' ||
                        elementType === 'Boolean' ||
                        elementType === 'Long') {
                        return null;
                    }
                }
            }

            if (isEntityCollection && isDtoCollection) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                let elementType = 'item';
                if (javaType) {
                    const elementTypeMatch = javaType.match(/<(.*)>/);
                    if (elementTypeMatch) {
                        elementType = elementTypeMatch[1];
                    }
                }
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map((${elementType} item) -> item.${extractMethod}).collect(${collector}) : null);`;
            } else {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${extractMethod} : null);`;
            }
        }

        // Default: Simple setter
        return `        dto.set${capName}(${getterCall});`;
    }

    /**
     * Builds a getter call for an entity property
     */
    buildEntityGetterCall(prop: any): string | null {
        if (!prop?.name) {
            return null;
        }
        const capName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
        return `get${capName}()`;
    }

    /**
     * Resolves DTO field mappings from entity's uses dto declarations
     */
    resolveDtoFieldMappings(entity: EntityExt): Map<string, { property: any; extractField?: string }> {
        const overrides = new Map<string, { property: any; extractField?: string }>();
        const fieldMappings = getEffectiveFieldMappings(entity);
        if (!fieldMappings || fieldMappings.length === 0) return overrides;

        const effectiveProps = getEffectiveProperties(entity);
        for (const mapping of fieldMappings) {
            if (!mapping?.dtoField || !mapping?.entityField) continue;
            const targetProp = effectiveProps.find((prop: any) => prop.name === mapping.entityField);
            if (targetProp) {
                overrides.set(mapping.dtoField, {
                    property: targetProp,
                    extractField: mapping.extractField
                });
            }
        }

        return overrides;
    }

    /**
     * Checks if a property is an enum type
     */
    isEnumProperty(prop: any): boolean {
        if (!prop?.type) {
            return false;
        }
        const javaType = TypeResolver.resolveJavaType(prop.type);
        return TypeResolver.isEnumType(javaType);
    }

    /**
     * Resolves the import path for a DTO type
     */
    resolveDtoImportPath(dtoType: string, projectName: string, owningAggregate?: string, dtoRegistry?: DtoSchemaRegistry): string | null {
        const dtoInfo = dtoRegistry?.dtoByName?.[dtoType];
        let targetAggregate = dtoInfo?.aggregateName;

        if (!targetAggregate && dtoType.endsWith('Dto')) {
            targetAggregate = dtoType.slice(0, -3);
        }

        if (!targetAggregate) {
            return null;
        }

        const config = getGlobalConfig();
        const dtoPackage = config.buildPackageName(projectName, 'shared', 'dtos');
        const importPath = `${dtoPackage}.${dtoType}`;
        return `import ${importPath};`;
    }

    /**
     * Adds required imports for all entities
     */
    addRequiredImports(isRootEntity: boolean, aggregateName: string): void {
        this.importManager.addJakartaImport('persistence.Entity');

        if (isRootEntity) {
            this.importManager.addBaseFrameworkImport('ms.domain.aggregate.Aggregate');
        }
    }
}
