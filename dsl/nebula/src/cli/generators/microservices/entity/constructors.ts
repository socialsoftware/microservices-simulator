import { Entity } from "../../../../language/generated/ast.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import { ImportRequirements } from "./types.js";
import type { DtoSchemaRegistry } from "../../../services/dto-schema-service.js";
import { getEffectiveFieldMappings, getEffectiveProperties } from "../../../utils/aggregate-helpers.js";

const resolveJavaType = (type: any, fieldName?: string) => {
    return TypeResolver.resolveJavaType(type);
};


const isEnumType = (type: any) => {
    if (type && typeof type === 'object' &&
        type.$type === 'EntityType' &&
        type.type) {
        if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
            return true;
        }
        if (type.type.ref && type.type.ref.$type === 'EnumDefinition') {
            return true;
        }
    }
    return false;
};

function convertDefaultValueToJava(defaultValue: any, javaType: string, prop: any): string {
    if (!defaultValue) {
        return 'null';
    }

    let valueText: string;
    if (typeof defaultValue === 'string') {
        valueText = defaultValue;
    } else if (defaultValue.$cstNode && defaultValue.$cstNode.text) {
        valueText = defaultValue.$cstNode.text.trim();
    } else if (defaultValue.$text) {
        valueText = defaultValue.$text.trim();
    } else {
        return 'null';
    }

    if (valueText === 'null') {
        return 'null';
    }

    if (valueText === '[]') {
        if (javaType.startsWith('List<')) {
            return 'new ArrayList<>()';
        } else if (javaType.startsWith('Set<')) {
            return 'new HashSet<>()';
        }
        return 'null';
    }

    if (valueText.startsWith('"') && valueText.endsWith('"')) {
        return valueText;
    }

    if (/^[A-Z][A-Z_0-9]*$/.test(valueText)) {
        return valueText;
    }

    if (valueText === 'true' || valueText === 'false') {
        return valueText;
    }

    if (/^-?[0-9]+(\.[0-9]+)?$/.test(valueText)) {
        return valueText;
    }

    return valueText;
}

export function generateDefaultConstructor(entity: Entity): { code: string } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;
    const effectiveProps = getEffectiveProperties(entity);

    const defaultInitializations = effectiveProps.map((prop: any, index: number) => {
        if (!isRootEntity && index === 0) {
            return '';
        }

        const isFinal = (prop as any).isFinal || false;
        const javaType = resolveJavaType(prop.type);
        const defaultValue = (prop as any).defaultValue;
        const isEnumType = (type: any) => {
            if (type && typeof type === 'object' &&
                type.$type === 'EntityType' &&
                type.type) {
                if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
                    return true;
                }
                if (type.type.ref && type.type.ref.$type === 'EnumDefinition') {
                    return true;
                }
            }
            return false;
        };
        const isEnum = isEnumType(prop.type) || javaType === 'AggregateState';
        const hasEnumDefault = defaultValue && isEnum;
        const hasFinalDefault = isFinal && defaultValue;

        if (isFinal) {
            if (hasFinalDefault || hasEnumDefault) {
                const javaDefaultValue = convertDefaultValueToJava(defaultValue, javaType, prop);
                if (javaDefaultValue && javaDefaultValue !== 'null') {
                    return `        this.${prop.name} = ${javaDefaultValue};`;
                }
            }
            return `        this.${prop.name} = null;`;
        }

        if (hasEnumDefault) {
            const javaDefaultValue = convertDefaultValueToJava(defaultValue, javaType, prop);
            if (javaDefaultValue && javaDefaultValue !== 'null') {
                return `        this.${prop.name} = ${javaDefaultValue};`;
            }
        }

        return '';
    }).filter(init => init !== '').join('\n');

    const constructorBody = defaultInitializations ? defaultInitializations : '';

    return {
        code: `\n    public ${entityName}() {\n${constructorBody}\n    }`
    };
}

export function generateEntityDtoConstructor(entity: Entity, projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry): { code: string, imports: ImportRequirements } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;

    const entityAny = entity as any;
    const aggregateRef = entityAny.aggregateRef as string | undefined;
    const dtoFieldMappingEntries: { dtoField: string; entityField: string; extractField?: string }[] =
        getEffectiveFieldMappings(entity)
            .filter((mapping: any) => mapping?.dtoField && mapping?.entityField)
            .map((mapping: any) => ({
                dtoField: mapping.dtoField,
                entityField: mapping.entityField,
                extractField: mapping.extractField
            }));
    const effectiveProps = getEffectiveProperties(entity);
    const dtoFieldToEntityProp = new Map<string, { property: any; extractField?: string }>();
    dtoFieldMappingEntries.forEach(entry => {
        const targetProp = effectiveProps.find((prop: any) => prop.name === entry.entityField);
        if (targetProp) {
            dtoFieldToEntityProp.set(entry.dtoField, { property: targetProp, extractField: entry.extractField });
        }
    });

    let dtoName: string;
    let dtoTypeName: string;

    if (aggregateRef) {
        // aggregateRef is the aggregate name (e.g., "Topic"), derive DTO name (e.g., "TopicDto")
        dtoName = `${aggregateRef}Dto`;
        dtoTypeName = dtoName;
    } else {
        if (isRootEntity) {
            dtoName = `${entityName}Dto`;
            dtoTypeName = dtoName;
        } else {
            dtoName = `${entityName}Dto`;
            dtoTypeName = dtoName;
        }
    }

    const dtoSchema = dtoSchemaRegistry?.dtoByName?.[dtoTypeName];
    if (!dtoSchema) {
        throw new Error(`DTO schema for ${dtoTypeName} was not found. Ensure a root entity defines this DTO.`);
    }
    const dtoParamName = aggregateRef
        ? `${aggregateRef.charAt(0).toLowerCase() + aggregateRef.slice(1)}Dto`
        : `${entityName.charAt(0).toLowerCase() + entityName.slice(1)}Dto`;

    // Find entity relationships (both single and collections) for internal DTO-to-entity conversion
    const singleEntityRels: Array<{ javaType: string; name: string; dtoType: string }> = [];
    const collectionEntityRels: Array<{ javaType: string; name: string; elementType: string; dtoElementType: string; collectionType: string }> = [];
    
    if (isRootEntity) {
        for (const prop of effectiveProps || []) {
            const javaType = resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            
            if (!isEnumType(prop.type) && TypeResolver.isEntityType(javaType)) {
                if (isCollection) {
                    const elementType = TypeResolver.getElementType(prop.type) || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                    const collectionType = javaType.startsWith('Set<') ? 'Set' : 'List';
                    collectionEntityRels.push({ 
                        javaType, 
                        name: prop.name, 
                        elementType,
                        dtoElementType: `${elementType}Dto`,
                        collectionType
                    });
                } else {
                    singleEntityRels.push({ 
                        javaType, 
                        name: prop.name,
                        dtoType: `${javaType}Dto`
                    });
                }
            }
        }
    }

    // SIMPLIFIED: Build parameter string - just (aggregateId, dto) for root entities
    const paramsParts: string[] = [];
    if (isRootEntity) {
        paramsParts.push('Integer aggregateId');
        paramsParts.push(`${dtoTypeName} ${dtoParamName}`);
    } else {
        paramsParts.push(`${dtoTypeName} ${dtoParamName}`);
    }
    
    const params = paramsParts.join(', ');

    const setterCalls: string[] = [];

    for (const field of dtoSchema.fields) {
        if (field.derivedAggregateId) {
            const mappingOverride = dtoFieldToEntityProp.get(field.name);
            if (!mappingOverride) {
                continue;
            }
        }
        if (field.isAggregateField) {
            const mappingOverride = dtoFieldToEntityProp.get(field.name);
            if (!mappingOverride) {
                continue;
            }
        }

        const mappingOverride = dtoFieldToEntityProp.get(field.name);
        const entityProp = mappingOverride?.property || field.sourceProperty;
        if (!entityProp) {
            continue;
        }
        const propertyBelongsToEntity = entityProp.$container?.name === entity.name;
        if (!propertyBelongsToEntity && !mappingOverride) {
            continue;
        }

        const propName = entityProp.name;
        const capitalizedName = propName.charAt(0).toUpperCase() + propName.slice(1);
        const isFinal = (entityProp as any).isFinal || false;
        const dtoGetterName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const javaType = resolveJavaType(entityProp.type);
        const isEntityRelationship =
            !isEnumType(entityProp.type) &&
            TypeResolver.isEntityType(javaType) &&
            !javaType.startsWith('Set<') &&
            !javaType.startsWith('List<');

        const effectiveExtractField = mappingOverride?.extractField || field.extractField;

        if (isRootEntity && (field.requiresConversion && field.referencedEntityName)) {
            continue;
        }

        if (isFinal) {
            if (field.requiresConversion && field.referencedEntityName) {
                setterCalls.push(`        this.${propName} = ${dtoParamName}.get${dtoGetterName}() != null ? new ${field.referencedEntityName}(${dtoParamName}.get${dtoGetterName}()) : null;`);
            } else if (isEnumType(entityProp.type)) {
                const defaultValue = (entityProp as any).defaultValue;
                const javaDefaultValue = defaultValue ? convertDefaultValueToJava(defaultValue, javaType, entityProp) : null;
                if (javaDefaultValue && javaDefaultValue !== 'null') {
                    setterCalls.push(`        this.${propName} = ${dtoParamName}.get${dtoGetterName}() != null ? ${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}()) : ${javaDefaultValue};`);
                } else {
                    setterCalls.push(`        this.${propName} = ${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}());`);
                }
            } else {
                setterCalls.push(`        this.${propName} = ${dtoParamName}.get${dtoGetterName}();`);
            }
            continue;
        }

        if (field.requiresConversion) {
            if (field.isCollection && field.referencedEntityName) {
                const collector = javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                if (effectiveExtractField) {
                    const extractMethod = `get${effectiveExtractField.charAt(0).toUpperCase() + effectiveExtractField.slice(1)}()`;
                    let elementType = 'dto';
                    if (field.javaType) {
                        const elementTypeMatch = field.javaType.match(/<(.*)>/);
                        if (elementTypeMatch) {
                            elementType = elementTypeMatch[1];
                        }
                    }
                    setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? ${dtoParamName}.get${dtoGetterName}().stream().map((${elementType} dto) -> dto.${extractMethod}).collect(${collector}) : null);`);
                } else {
                    let elementType = 'dto';
                    if (field.javaType) {
                        const elementTypeMatch = field.javaType.match(/<(.*)>/);
                        if (elementTypeMatch) {
                            elementType = elementTypeMatch[1];
                        }
                    }
                    setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? ${dtoParamName}.get${dtoGetterName}().stream().map((${elementType} dto) -> new ${field.referencedEntityName}(dto)).collect(${collector}) : null);`);
                }
            } else if (!field.isCollection && field.referencedEntityName) {
                setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? new ${field.referencedEntityName}(${dtoParamName}.get${dtoGetterName}()) : null);`);
            } else {
                setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`);
            }
            continue;
        }

        if (isEnumType(entityProp.type)) {
            const defaultValue = (entityProp as any).defaultValue;
            const javaDefaultValue = defaultValue ? convertDefaultValueToJava(defaultValue, javaType, entityProp) : null;
            if (javaDefaultValue && javaDefaultValue !== 'null') {
                setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? ${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}()) : ${javaDefaultValue});`);
            } else {
                setterCalls.push(`        set${capitalizedName}(${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}()));`);
            }
            continue;
        }

        if (!isRootEntity && isEntityRelationship && field.referencedEntityName) {
            setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? new ${field.referencedEntityName}(${dtoParamName}.get${dtoGetterName}()) : null);`);
            continue;
        }

        if (effectiveExtractField) {
            const extractMethod = `get${effectiveExtractField.charAt(0).toUpperCase() + effectiveExtractField.slice(1)}()`;
            const isDtoCollection = field.isCollection;
            const isEntityCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');

            if (isDtoCollection && isEntityCollection) {
                const collector = javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                let elementType = 'item';
                if (field.javaType) {
                    const elementTypeMatch = field.javaType.match(/<(.*)>/);
                    if (elementTypeMatch) {
                        elementType = elementTypeMatch[1];
                    }
                }
                setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? ${dtoParamName}.get${dtoGetterName}().stream().map((${elementType} item) -> item.${extractMethod}).collect(${collector}) : null);`);
            } else {
                setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? ${dtoParamName}.get${dtoGetterName}().${extractMethod} : null);`);
            }
        } else {
            setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`);
        }
    }

    // SIMPLIFIED: Convert nested DTOs to entities (single entities first, then collections)
    if (isRootEntity) {
        // Convert single entity DTOs to entities
        for (const rel of singleEntityRels) {
            const prop = effectiveProps.find((p: any) => p.name === rel.name);
            if (prop) {
                const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                const isFinalProp = (prop as any).isFinal || false;
                // Convert from DTO: new EntityType(dto.getEntityDto())
                if (isFinalProp) {
                    setterCalls.push(`        this.${prop.name} = ${dtoParamName}.get${capitalizedName}() != null ? new ${rel.javaType}(${dtoParamName}.get${capitalizedName}()) : null;`);
                } else {
                    setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${capitalizedName}() != null ? new ${rel.javaType}(${dtoParamName}.get${capitalizedName}()) : null);`);
                }
            }
        }
        
        // Convert collection entity DTOs to entities
        for (const rel of collectionEntityRels) {
            const prop = effectiveProps.find((p: any) => p.name === rel.name);
            if (prop) {
                const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                const isFinalProp = (prop as any).isFinal || false;
                const collector = rel.collectionType === 'Set' ? 'Collectors.toSet()' : 'Collectors.toList()';
                // Convert from DTO collection: dto.getEntities().stream().map(EntityType::new).collect(...)
                if (isFinalProp) {
                    setterCalls.push(`        this.${prop.name} = ${dtoParamName}.get${capitalizedName}() != null ? ${dtoParamName}.get${capitalizedName}().stream().map(${rel.elementType}::new).collect(${collector}) : null;`);
                } else {
                    setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${capitalizedName}() != null ? ${dtoParamName}.get${capitalizedName}().stream().map(${rel.elementType}::new).collect(${collector}) : null);`);
                }
            }
        }
    }

    const constructorBody = isRootEntity ?
        `        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
${setterCalls.join('\n')}` :
        setterCalls.join('\n');

    const code = `\n    public ${entityName}(${params}) {
${constructorBody}
    }`;

    const imports: ImportRequirements = {};
    addDtoImport(dtoTypeName, entity, projectName, dtoSchemaRegistry, imports);

    for (const field of dtoSchema.fields) {
        if (field.sourceProperty && isEnumType(field.sourceProperty.type)) {
            const enumType = resolveJavaType(field.sourceProperty.type);
            const enumImportPath = `${getGlobalConfig().buildPackageName(projectName, 'shared', 'enums')}.${enumType}`;
            if (!imports.customImports) imports.customImports = new Set();
            imports.customImports.add(`import ${enumImportPath};`);
        }
    }

    if (setterCalls.some(call => call.includes('Collectors.'))) {
        imports.usesCollectors = true;
    }

    return {
        code,
        imports
    };
}

function addDtoImport(
    dtoTypeName: string,
    _entity: Entity,
    projectName: string,
    _dtoSchemaRegistry: DtoSchemaRegistry | undefined,
    imports: ImportRequirements
): void {
    if (!imports.customImports) {
        imports.customImports = new Set<string>();
    }

    const config = getGlobalConfig();
    const dtoPackage = config.buildPackageName(projectName, 'shared', 'dtos');
    imports.customImports.add(`import ${dtoPackage}.${dtoTypeName};`);
}

/**
 * Generate a constructor that accepts the entity's own projection DTO.
 * This is needed for non-root entities with aggregateRef, which already have a constructor
 * for the referenced aggregate's DTO (e.g., CourseDto), but also need one for their own DTO
 * (e.g., ExecutionCourseDto) so the root aggregate can create them from nested DTOs.
 */
export function generateProjectionDtoConstructor(entity: Entity, projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry): { code: string, imports: ImportRequirements } | null {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;
    const entityAny = entity as any;
    const aggregateRef = entityAny.aggregateRef as string | undefined;

    // Only needed for non-root entities that have an aggregateRef
    // (these entities have a constructor for the referenced aggregate DTO, but need another for their own DTO)
    if (isRootEntity || !aggregateRef) {
        return null;
    }

    const projectionDtoName = `${entityName}Dto`;
    const projectionDtoParamName = `${entityName.charAt(0).toLowerCase() + entityName.slice(1)}Dto`;

    const dtoSchema = dtoSchemaRegistry?.dtoByName?.[projectionDtoName];
    if (!dtoSchema) {
        return null;
    }

    const effectiveProps = getEffectiveProperties(entity);
    const setterCalls: string[] = [];

    for (const field of dtoSchema.fields) {
        // Find the entity property that maps to this DTO field
        // The mapping is: dtoField.sourceName -> entity property name
        const entityProp = effectiveProps.find((p: any) => p.name === field.sourceName || p.name === field.name);
        if (!entityProp) continue;

        // Use entity property name for setter, but DTO field name for getter
        const entityPropName = entityProp.name;
        const capitalizedEntityPropName = entityPropName.charAt(0).toUpperCase() + entityPropName.slice(1);
        
        // DTO getter uses the DTO field name
        const dtoFieldName = field.name;
        const capitalizedDtoFieldName = dtoFieldName.charAt(0).toUpperCase() + dtoFieldName.slice(1);
        
        const javaType = resolveJavaType(entityProp.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
        const isEntityType = !isEnumType(entityProp.type) && TypeResolver.isEntityType(javaType);

        if (isEnumType(entityProp.type)) {
            setterCalls.push(`        set${capitalizedEntityPropName}(${projectionDtoParamName}.get${capitalizedDtoFieldName}() != null ? ${javaType}.valueOf(${projectionDtoParamName}.get${capitalizedDtoFieldName}()) : null);`);
        } else if (isEntityType && !isCollection) {
            // For single entity relationships, convert DTO to entity
            setterCalls.push(`        set${capitalizedEntityPropName}(${projectionDtoParamName}.get${capitalizedDtoFieldName}() != null ? new ${javaType}(${projectionDtoParamName}.get${capitalizedDtoFieldName}()) : null);`);
        } else if (isEntityType && isCollection) {
            // For collection entity relationships, convert each DTO to entity
            const collectionType = javaType.startsWith('Set<') ? 'Set' : 'List';
            const elementType = TypeResolver.getElementType(entityProp.type) || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
            setterCalls.push(`        set${capitalizedEntityPropName}(${projectionDtoParamName}.get${capitalizedDtoFieldName}() != null ? ${projectionDtoParamName}.get${capitalizedDtoFieldName}().stream().map(${elementType}::new).collect(Collectors.to${collectionType}()) : null);`);
        } else {
            setterCalls.push(`        set${capitalizedEntityPropName}(${projectionDtoParamName}.get${capitalizedDtoFieldName}());`);
        }
    }

    const code = `\n    public ${entityName}(${projectionDtoName} ${projectionDtoParamName}) {
${setterCalls.join('\n')}
    }`;

    const imports: ImportRequirements = {};
    const config = getGlobalConfig();
    const dtoPackage = config.buildPackageName(projectName, 'shared', 'dtos');
    imports.customImports = new Set([`import ${dtoPackage}.${projectionDtoName};`]);

    return { code, imports };
}

export function generateCopyConstructor(entity: Entity): { code: string, imports: ImportRequirements } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;
    const imports: ImportRequirements = {};
    const effectiveProps = getEffectiveProperties(entity);

    const setterCalls = effectiveProps.map((prop: any, index: number) => {
        if (!isRootEntity && index === 0) {
            return '';
        }

        const javaType = resolveJavaType(prop.type);
        const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
        const isFinal = (prop as any).isFinal || false;

        if (isFinal) {
            return `        this.${prop.name} = other.get${capitalizedName}();`;
        }

        if (TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
            return `        set${capitalizedName}(new ${javaType}(other.get${capitalizedName}()));`;
        } else if (javaType.startsWith('Set<')) {
            const elementType = TypeResolver.getElementType(prop.type);
            if (elementType && TypeResolver.isEntityType(elementType)) {
                imports.usesCollectors = true;
                return `        set${capitalizedName}(other.get${capitalizedName}().stream().map(${elementType}::new).collect(Collectors.toSet()));`;
            } else {
                imports.usesSet = true;
                return `        set${capitalizedName}(new HashSet<>(other.get${capitalizedName}()));`;
            }
        } else if (javaType.startsWith('List<')) {
            const elementType = TypeResolver.getElementType(prop.type);
            if (elementType && TypeResolver.isEntityType(elementType)) {
                imports.usesCollectors = true;
                return `        set${capitalizedName}(other.get${capitalizedName}().stream().map(${elementType}::new).collect(Collectors.toList()));`;
            } else {
                imports.usesList = true;
                return `        set${capitalizedName}(new ArrayList<>(other.get${capitalizedName}()));`;
            }
        } else {
            return `        set${capitalizedName}(other.get${capitalizedName}());`;
        }
    }).filter(call => call !== '').join('\n');

    const constructorBody = isRootEntity ?
        `        super(other);
${setterCalls}` :
        setterCalls;

    const code = `\n    public ${entityName}(${entityName} other) {
${constructorBody}
    }`;

    return {
        code,
        imports
    };
}
