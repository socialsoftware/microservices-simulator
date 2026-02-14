import { Entity } from "../../../../../language/generated/ast.js";
import { EntityExt } from "../../../../types/ast-extensions.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { getGlobalConfig } from "../../../common/config.js";
import { getEffectiveProperties, getEffectiveFieldMappings, getAllModels } from "../../../../utils/aggregate-helpers.js";
import { ImportManager } from "../../../../utils/import-manager.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../../services/dto-schema-service.js";
import { TypeExtractor } from "../../../common/utils/type-extractor.js";
import { DtoSetterBuilder } from "./dto-setter-strategies/dto-setter-builder.js";
import type { StrategyContext } from "./dto-setter-strategies/dto-setter-strategy.js";
import { EntityPatternDetector } from "../../../common/utils/entity-pattern-detector.js";

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
export class ImportScanner implements StrategyContext {
    private dtoSetterBuilder: DtoSetterBuilder;

    constructor(
        private importManager: ImportManager,
        private dtoRegistry: DtoSchemaRegistry
    ) {
        this.dtoSetterBuilder = new DtoSetterBuilder(this);
    }

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

        // Entity imports - dynamically detect based on all aggregates
        const allModels = getAllModels();
        const allAggregates = allModels.flatMap(model => model.aggregates);
        const entityPattern = EntityPatternDetector.buildEntityPattern(allAggregates);
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
        const foundEnums = TypeExtractor.extractEnumsFromCode(javaCode);

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
    /**
     * Builds a DTO setter statement for a field.
     *
     * Refactored to use Strategy pattern for maintainability.
     * Each case is handled by a dedicated strategy class.
     */
    buildDtoSetterFromSchema(
        field: DtoFieldSchema,
        entity: EntityExt,
        dtoFieldOverrides?: Map<string, { property: any; extractField?: string }>
    ): string | null {
        const override = dtoFieldOverrides?.get(field.name);
        const isRootEntity = entity.isRoot || false;

        // Early return for non-root aggregate fields
        if (field.isAggregateField && !override && !isRootEntity) {
            return null;
        }

        // Find the property for this field
        const effectiveProps = getEffectiveProperties(entity);
        const prop = override?.property || effectiveProps.find((p: any) => p.name === (field.sourceName || field.name));
        if (!prop && !field.isAggregateField) {
            return null;
        }

        // Check if property belongs to this entity
        if (prop) {
            const belongsToEntity = prop.$container?.name === entity.name || (prop as any).$fromMapping;
            if (!belongsToEntity && !override) {
                return null;
            }
        }

        // Build getter call
        const getterCall = prop ? this.buildEntityGetterCall(prop) : '';
        if (!getterCall && !field.isAggregateField) {
            return null;
        }

        // Use strategy pattern to build setter
        // At this point, getterCall is guaranteed to be non-null or we have an aggregate field
        return this.dtoSetterBuilder.buildSetter(field, entity, prop, override, getterCall || '');
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
     * Resolves DTO field mappings from entity's cross-aggregate field mappings
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
