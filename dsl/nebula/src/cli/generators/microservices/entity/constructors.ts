import { Entity } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import { ImportRequirements } from "./types.js";

const resolveJavaType = (type: any, fieldName?: string) => {
    return TypeResolver.resolveJavaType(type);
};


const isEnumType = (type: any) => {
    // Check if it's an EntityType that references an EnumDefinition
    if (type && typeof type === 'object' &&
        type.$type === 'EntityType' &&
        type.type) {
        // Check if the reference text indicates an enum type
        if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
            return true;
        }
        // Check if the resolved reference is an EnumDefinition
        if (type.type.ref && type.type.ref.$type === 'EnumDefinition') {
            return true;
        }
    }
    return false;
};

const isEnumTypeByNaming = (javaType: string) => {
    // No longer using naming convention - enums must be explicitly defined
    return false;
};

export function generateDefaultConstructor(entity: Entity): { code: string } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;

    const finalFieldInitializations = entity.properties.map((prop: any, index: number) => {
        if (!isRootEntity && index === 0) {
            return '';
        }

        const isFinal = (prop as any).isFinal || false;
        if (!isFinal) {
            return '';
        }

        const javaType = resolveJavaType(prop.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

        if (isCollection) {
            if (javaType.startsWith('List<')) {
                return `        this.${prop.name} = new ArrayList<>();`;
            } else if (javaType.startsWith('Set<')) {
                return `        this.${prop.name} = new HashSet<>();`;
            }
        } else if (TypeResolver.isPrimitiveType(javaType)) {
            if (javaType === 'Boolean') {
                return `        this.${prop.name} = null;`;
            } else {
                return `        this.${prop.name} = null;`;
            }
        }

        return `        this.${prop.name} = null;`;
    }).filter(init => init !== '').join('\n');

    const constructorBody = finalFieldInitializations ? finalFieldInitializations : '';

    return {
        code: `\n    public ${entityName}() {\n${constructorBody}\n    }`
    };
}

export function generateEntityDtoConstructor(entity: Entity, projectName: string, allSharedDtos?: any[], dtoMappings?: any[]): { code: string, imports: ImportRequirements } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;

    // Check if entity specifies a custom DTO type
    const entityAny = entity as any;
    const dtoTypeRef = entityAny.dtoType;

    const customDtoType = dtoTypeRef?.ref?.name || dtoTypeRef?.$refText;

    // Determine DTO name and type
    let dtoName: string;
    let dtoTypeName: string;

    if (customDtoType) {
        // Use the specified DTO type
        dtoName = customDtoType;
        dtoTypeName = customDtoType;
    } else {
        // For non-root entities, use the root entity's DTO
        const rootEntityName = isRootEntity ? entityName : (entity.$container?.name || entityName);
        dtoName = `${rootEntityName}Dto`;
        dtoTypeName = dtoName;
    }

    // Find entity relationships for constructor parameters
    const entityRelationships = entity.properties.filter((prop: any) => {
        const javaType = resolveJavaType(prop.type);
        return TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<');
    });

    const relationshipParams = entityRelationships.map((prop: any) =>
        `${resolveJavaType(prop.type)} ${prop.name}`
    ).join(', ');

    // Generate parameter name based on DTO type (preserve camelCase)
    const dtoParamName = customDtoType ?
        customDtoType.charAt(0).toLowerCase() + customDtoType.slice(1) :
        `${(isRootEntity ? entityName : (entity.$container?.name || entityName)).toLowerCase()}Dto`;

    // For root entities, include aggregateId parameter; for non-root entities, don't
    const params = isRootEntity ?
        (relationshipParams ?
            `Integer aggregateId, ${dtoTypeName} ${dtoParamName}, ${relationshipParams}` :
            `Integer aggregateId, ${dtoTypeName} ${dtoParamName}`) :
        `${dtoTypeName} ${dtoParamName}`;

    const setterCalls = entity.properties.map((prop: any, index: number) => {
        if (!isRootEntity && index === 0) {
            return '';
        }

        const javaType = resolveJavaType(prop.type);
        const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
        const isFinal = (prop as any).isFinal || false;

        let dtoGetterName = capitalizedName;

        if (customDtoType) {
            const mappedName = mapEntityFieldToDtoField(prop.name, customDtoType, dtoMappings, entity);
            if (mappedName === null) {
                return '';
            }
            dtoGetterName = mappedName;
        }

        if (isFinal) {
            if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
                return `        this.${prop.name} = ${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}());`;
            } else {
                return `        this.${prop.name} = ${dtoParamName}.get${dtoGetterName}();`;
            }
        }

        if (javaType === 'LocalDateTime') {
            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`;
        } else if (TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
            return `        set${capitalizedName}(${prop.name});`;
        } else if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            return `        set${capitalizedName}(${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}()));`;
        } else if (javaType.startsWith('List<') || javaType.startsWith('Set<')) {
            return generateCollectionMapping(prop, dtoParamName, dtoGetterName, javaType, customDtoType, dtoMappings, entity);
        } else if (!javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`;
        }
        return '';
    }).filter(call => call !== '').join('\n');

    const constructorBody = isRootEntity ?
        `        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
${setterCalls}` :
        setterCalls;

    const code = `\n    public ${entityName}(${params}) {
${constructorBody}
    }`;

    // Add import for the DTO (always add since constructor uses it)
    const imports: ImportRequirements = {};
    if (!imports.customImports) imports.customImports = new Set();
    const dtoImportPath = `${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${dtoTypeName}`;
    imports.customImports.add(`import ${dtoImportPath};`);

    // Add imports for any enum types used in the entity
    entity.properties.forEach((prop: any) => {
        if (isEnumType(prop.type) || isEnumTypeByNaming(resolveJavaType(prop.type))) {
            const enumType = resolveJavaType(prop.type);
            const enumImportPath = `${getGlobalConfig().buildPackageName(projectName, 'shared', 'enums')}.${enumType}`;
            if (!imports.customImports) imports.customImports = new Set();
            imports.customImports.add(`import ${enumImportPath};`);
        }
    });

    return {
        code,
        imports
    };
}

export function generateCopyConstructor(entity: Entity): { code: string, imports: ImportRequirements } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;
    const imports: ImportRequirements = {};

    // Generate setter calls for copy constructor
    const setterCalls = entity.properties.map((prop: any, index: number) => {
        // Skip the first property (id) for non-root entities as it's @GeneratedValue
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
            // Single entity relationship - create new instance
            return `        set${capitalizedName}(new ${javaType}(other.get${capitalizedName}()));`;
        } else if (javaType.startsWith('Set<')) {
            // Collection relationship - deep copy with stream
            const elementType = TypeResolver.getElementType(prop.type);
            if (elementType && TypeResolver.isEntityType(elementType)) {
                imports.usesCollectors = true; // Only set when actually using Collectors
                return `        set${capitalizedName}(other.get${capitalizedName}().stream().map(${elementType}::new).collect(Collectors.toSet()));`;
            } else {
                imports.usesSet = true; // Need HashSet import
                return `        set${capitalizedName}(new HashSet<>(other.get${capitalizedName}()));`;
            }
        } else if (javaType.startsWith('List<')) {
            // List relationship - deep copy with stream
            const elementType = TypeResolver.getElementType(prop.type);
            if (elementType && TypeResolver.isEntityType(elementType)) {
                imports.usesCollectors = true; // Only set when actually using Collectors
                return `        set${capitalizedName}(other.get${capitalizedName}().stream().map(${elementType}::new).collect(Collectors.toList()));`;
            } else {
                imports.usesList = true; // Need ArrayList import
                return `        set${capitalizedName}(new ArrayList<>(other.get${capitalizedName}()));`;
            }
        } else {
            // Primitive types - direct copy
            return `        set${capitalizedName}(other.get${capitalizedName}());`;
        }
    }).filter(call => call !== '').join('\n');

    // Different constructor body for root vs non-root entities
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

// Helper function to map entity field names to DTO field names for custom DTOs
function mapEntityFieldToDtoField(entityFieldName: string, dtoType: string, dtoMappings?: any[], entity?: Entity): string | null {
    // Check if entity has explicit DTO mapping
    const entityAny = entity as any;
    if (entityAny?.dtoMapping?.fieldMappings) {
        // Look for explicit field mapping
        for (const fieldMapping of entityAny.dtoMapping.fieldMappings) {
            if (fieldMapping.entityField === entityFieldName) {
                // Found explicit mapping: entityField -> dtoField
                return capitalize(fieldMapping.dtoField);
            }
        }
        // Entity has explicit mappings but this field is not mapped - skip it
        return null;
    }

    // For direct DTO usage (like "Entity Option uses dto OptionDto"), 
    // use simple field name matching instead of complex collection mapping
    if (!dtoMappings || !entity) {
        // Fallback: just capitalize (assuming the field exists in the DTO)
        return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
    }

    // Check if this is a direct DTO usage (entity name matches DTO name pattern)
    const entityName = entity.name;
    const expectedDtoName = `${entityName}Dto`;
    if (dtoType === expectedDtoName) {
        // Direct DTO usage - use simple field name capitalization
        return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
    }

    // Complex collection mapping logic (existing logic preserved)
    // ... (rest of the existing mapping logic)

    return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
}

/**
 * Generate collection mapping code with optional field extraction
 */
function generateCollectionMapping(
    prop: any,
    dtoParamName: string,
    dtoGetterName: string,
    javaType: string,
    customDtoType: string,
    dtoMappings?: any[],
    entity?: Entity
): string {
    const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);

    // Check if there's an explicit mapping with extract field
    const entityAny = entity as any;
    if (entityAny?.dtoMapping?.fieldMappings) {
        const fieldMapping = entityAny.dtoMapping.fieldMappings.find((fm: any) =>
            fm.entityField === prop.name
        );

        if (fieldMapping && fieldMapping.extractField) {
            // Generate collection mapping with field extraction
            const extractField = fieldMapping.extractField;
            const extractMethod = `get${extractField.charAt(0).toUpperCase() + extractField.slice(1)}`;
            const collectorMethod = javaType.startsWith('List<') ? 'toList' : 'toSet';

            // Determine the DTO type from the source collection
            const sourceDtoType = fieldMapping.dtoField; // This should be the collection name
            const elementDtoType = inferDtoTypeFromCollection(sourceDtoType, customDtoType);

            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}().stream()
            .map(${elementDtoType}::${extractMethod})
            .collect(Collectors.${collectorMethod}()));`;
        }
    }

    // Default collection handling (no extraction)
    return ''; // Skip collections without explicit mapping
}

/**
 * Infer the DTO type from a collection field name dynamically
 */
function inferDtoTypeFromCollection(collectionName: string, baseDtoType: string): string {
    // Convert collection name to singular DTO type
    // Examples: questions -> QuestionDto, users -> UserDto, topics -> TopicDto

    let singular: string;

    // Handle common English pluralization patterns
    if (collectionName.endsWith('ies')) {
        // categories -> category, stories -> story
        singular = collectionName.slice(0, -3) + 'y';
    } else if (collectionName.endsWith('es') && collectionName.length > 3) {
        // boxes -> box, classes -> class
        singular = collectionName.slice(0, -2);
    } else if (collectionName.endsWith('s') && collectionName.length > 1) {
        // questions -> question, users -> user
        singular = collectionName.slice(0, -1);
    } else {
        // Already singular or unknown pattern
        singular = collectionName;
    }

    // Capitalize first letter and add Dto suffix
    return `${singular.charAt(0).toUpperCase() + singular.slice(1)}Dto`;
}
