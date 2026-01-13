import { Entity } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../common/config.js";
import { EntityGenerationOptions } from "./types.js";
import { generateFields } from "./fields.js";
import { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor } from "./constructors.js";
import { generateGettersSetters, generateBackReferenceGetterSetter } from "./methods.js";
import { generateInvariants } from "./invariants.js";
import { ImportManager, ImportManagerFactory } from "../../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../../utils/error-handler.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../services/dto-schema-service.js";

// ============================================================================
// ENTITY GENERATION ORCHESTRATION
// ============================================================================

export class EntityOrchestrator {
    private importManager: ImportManager;
    private dtoRegistry?: DtoSchemaRegistry;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
    }

    generateEntityCode(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
        return ErrorHandler.wrap(
            () => this.generateEntityCodeInternal(entity, projectName, options),
            ErrorUtils.entityContext(
                'generate entity code',
                entity.$container?.name || 'unknown',
                entity.name,
                'entity-orchestrator',
                { isRoot: entity.isRoot, projectName }
            ),
            ErrorSeverity.FATAL
        ) || '';
    }

    private generateEntityCodeInternal(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
        const opts = options || { projectName };
        const isRootEntity = entity.isRoot || false;

        this.dtoRegistry = opts.dtoSchemaRegistry;

        const components = this.generateEntityComponents(entity, projectName, opts, isRootEntity);

        const classStructure = this.buildClassStructure(entity, projectName, isRootEntity);

        const javaCode = this.assembleJavaCode(classStructure, components, entity.name);

        return this.finalizeWithImports(javaCode, projectName, isRootEntity, classStructure.aggregateName, entity.name, entity);
    }

    private generateEntityComponents(entity: Entity, projectName: string, opts: EntityGenerationOptions, isRootEntity: boolean) {
        return {
            fields: generateFields(entity.properties, entity, isRootEntity, projectName).code,
            defaultConstructor: generateDefaultConstructor(entity).code,
            dtoConstructor: generateEntityDtoConstructor(entity, projectName, this.dtoRegistry).code,
            copyConstructor: generateCopyConstructor(entity).code,
            gettersSetters: generateGettersSetters(entity.properties, entity, projectName, opts.allEntities).code,
            backRefGetterSetter: (!isRootEntity && entity.$container)
                ? generateBackReferenceGetterSetter(entity.$container.name)
                : '',
            invariants: isRootEntity ? generateInvariants(entity).code : '',
            // All entities now get their own DTOs, so all need buildDto() method
            buildDtoMethod: this.generateBuildDtoMethod(entity)
        };
    }

    private buildClassStructure(entity: Entity, projectName: string, isRootEntity: boolean) {
        const aggregateName = entity.$container?.name || 'unknown';
        const config = getGlobalConfig();

        return {
            aggregateName,
            extendsClause: isRootEntity
                ? (aggregateName !== entity.name ? ` extends ${aggregateName}` : ' extends Aggregate')
                : '',
            abstractModifier: isRootEntity ? 'abstract ' : '',
            packageName: config.buildPackageName(
                projectName,
                'microservices',
                aggregateName.toLowerCase(),
                'aggregate'
            )
        };
    }

    private assembleJavaCode(classStructure: any, components: any, entityName: string): string {
        return `package ${classStructure.packageName};

IMPORTS_PLACEHOLDER

@Entity
public ${classStructure.abstractModifier}class ${entityName}${classStructure.extendsClause} {
${components.fields}
${components.defaultConstructor}
${components.dtoConstructor}
${components.copyConstructor}
${components.gettersSetters}
${components.backRefGetterSetter}
${components.invariants}
${components.buildDtoMethod}
}`;
    }

    private generateBuildDtoMethod(entity: Entity): string {
        const entityName = entity.name;

        // All entities now get their own DTOs, so all entities should have a buildDto() method
        const dtoTypeName = `${entityName}Dto`;
        const dtoSchema = this.dtoRegistry?.dtoByName?.[dtoTypeName];

        if (!dtoSchema) {
            // Fallback: generate simple constructor-based buildDto if no schema found
            return `\n    public ${dtoTypeName} buildDto() {\n        return new ${dtoTypeName}(this);\n    }`;
        }

        const dtoFieldOverrides = this.resolveDtoFieldMappings(entity);
        const setterLines = dtoSchema.fields
            .map(field => this.buildDtoSetterFromSchema(field, entity, dtoFieldOverrides))
            .filter((line): line is string => !!line);

        return `\n    public ${dtoTypeName} buildDto() {\n        ${dtoTypeName} dto = new ${dtoTypeName}();\n${setterLines.join('\n')}\n        return dto;\n    }`;
    }

    private finalizeWithImports(javaCode: string, projectName: string, isRootEntity: boolean, aggregateName: string, entityName: string, entity: Entity): string {
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

    private scanCodeForImports(javaCode: string, projectName: string, isRoot: boolean, aggregateName?: string, entityName?: string, entity?: Entity): string[] {
        const imports: string[] = [];
        const dtoRegistry = this.dtoRegistry;

        if (javaCode.includes('@Entity')) imports.push('import jakarta.persistence.Entity;');
        if (javaCode.includes('@Id')) imports.push('import jakarta.persistence.Id;');
        if (javaCode.includes('@GeneratedValue')) imports.push('import jakarta.persistence.GeneratedValue;');
        if (javaCode.includes('@OneToOne')) imports.push('import jakarta.persistence.OneToOne;');
        if (javaCode.includes('@OneToMany')) imports.push('import jakarta.persistence.OneToMany;');
        if (javaCode.includes('CascadeType')) imports.push('import jakarta.persistence.CascadeType;');
        if (javaCode.includes('FetchType')) imports.push('import jakarta.persistence.FetchType;');
        if (javaCode.includes('@Enumerated')) imports.push('import jakarta.persistence.Enumerated;');
        if (javaCode.includes('EnumType')) imports.push('import jakarta.persistence.EnumType;');

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

        if (javaCode.includes('AggregateState')) {
            const aggregateStateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate.AggregateState;`;
            imports.push(aggregateStateImport);
        }

        if (isRoot) {
            const aggregateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate;`;
            imports.push(aggregateImport);
        }

        const dtoPattern = /\b([A-Z][a-zA-Z]*Dto)\b/g;
        let dtoMatch;
        while ((dtoMatch = dtoPattern.exec(javaCode)) !== null) {
            const dtoType = dtoMatch[1];
            const importPath = this.resolveDtoImportPath(dtoType, projectName, aggregateName, dtoRegistry);
            if (importPath) {
                imports.push(importPath);
            }
        }

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

    private buildDtoSetterFromSchema(
        field: DtoFieldSchema,
        entity: Entity,
        dtoFieldOverrides?: Map<string, { property: any; extractField?: string }>
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const override = dtoFieldOverrides?.get(field.name);
        const isRootEntity = entity.isRoot || false;

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

        const prop = override?.property || entity.properties.find(p => p.name === (field.sourceName || field.name));
        if (!prop) {
            return null;
        }
        const belongsToEntity = prop.$container?.name === entity.name;
        if (!belongsToEntity && !override) {
            return null;
        }

        const getterCall = this.buildEntityGetterCall(prop);
        if (!getterCall) {
            return null;
        }

        const effectiveExtractField = override?.extractField || field.extractField;

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

        if (field.derivedAggregateId && field.sourceProperty) {
            const accessor = field.derivedAccessor || 'getAggregateId';
            if (field.isCollection) {
                // For collections, stream and map to extract aggregateIds
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(item -> item.${accessor}()).collect(${collector}) : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${accessor}() : null);`;
        }

        // Check if this is an enum field - convert to string using .name()
        // Check field.isEnum first (from schema), then fall back to property check
        if (field.isEnum || (prop && this.isEnumProperty(prop))) {
            if (field.isCollection) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(value -> value != null ? value.name() : null).collect(${collector}) : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
        }

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

        if (prop.name.endsWith('Type')) {
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.toString() : null);`;
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

    private buildEntityGetterCall(prop: any): string | null {
        if (!prop?.name) {
            return null;
        }
        const capName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
        return `get${capName}()`;
    }

    private resolveDtoFieldMappings(entity: Entity): Map<string, { property: any; extractField?: string }> {
        const overrides = new Map<string, { property: any; extractField?: string }>();
        const entityAny = entity as any;
        const fieldMappings = entityAny?.dtoMapping?.fieldMappings as any[] | undefined;
        if (!fieldMappings) {
            return overrides;
        }

        for (const mapping of fieldMappings) {
            if (!mapping?.dtoField || !mapping?.entityField) continue;
            const targetProp = entity.properties.find(prop => prop.name === mapping.entityField);
            if (targetProp) {
                overrides.set(mapping.dtoField, {
                    property: targetProp,
                    extractField: mapping.extractField
                });
            }
        }

        return overrides;
    }

    private isEnumProperty(prop: any): boolean {
        if (!prop?.type) {
            return false;
        }
        const javaType = TypeResolver.resolveJavaType(prop.type);
        return TypeResolver.isEnumType(javaType);
    }

    private resolveDtoImportPath(dtoType: string, projectName: string, owningAggregate?: string, dtoRegistry?: DtoSchemaRegistry): string | null {
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

    private addRequiredImports(isRootEntity: boolean, aggregateName: string): void {
        this.importManager.addJakartaImport('persistence.Entity');

        if (isRootEntity) {
            this.importManager.addBaseFrameworkImport('ms.domain.aggregate.Aggregate');
        }
    }
}



/**
 * Entity generator facade that uses the new orchestrator
 */
export class EntityGenerator {
    private orchestrator: EntityOrchestrator;

    constructor(projectName: string = 'project') {
        this.orchestrator = new EntityOrchestrator(projectName);
    }

    /**
     * Main entry point for entity generation
     */
    async generateEntity(entity: Entity, options: EntityGenerationOptions): Promise<string> {
        return this.orchestrator.generateEntityCode(entity, options.projectName, options);
    }
}