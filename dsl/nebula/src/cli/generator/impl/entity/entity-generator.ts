import { Entity } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../base/type-resolver.js";
import { getGlobalConfig } from "../../base/config.js";

const resolveJavaType = (type: any, fieldName?: string) => {
    const javaType = TypeResolver.resolveJavaType(type);

    // Legacy: Map String fields ending with "Type" to corresponding enums (for backward compatibility)
    if (javaType === 'String' && fieldName && fieldName.endsWith('Type')) {
        const enumName = fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
        return enumName;
    }

    return javaType;
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
    // Check if this follows enum naming convention (ends with "Type")
    return javaType.match(/^[A-Z][a-zA-Z]*Type$/) !== null;
};

// Import the shared DTO generator to check if a DTO is shared
import { SharedDtoGenerator } from '../shared/shared-dto-generator.js';

const getSharedDtoImportPath = (dtoName: string, projectName: string, allSharedDtos?: any[]): string | null => {
    // Use the dynamic SharedDtoGenerator.isSharedDto method
    if (SharedDtoGenerator.isSharedDto(dtoName, allSharedDtos)) {
        return `${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${dtoName}`;
    }
    return null;
};
const resolveParamReturnType = (type: any) => TypeResolver.resolveJavaType(type);

export type ImportRequirements = {
    usesPersistence?: boolean;
    usesLocalDateTime?: boolean;
    usesBigDecimal?: boolean;
    usesSet?: boolean;
    usesList?: boolean;
    usesAggregate?: boolean;
    usesStreams?: boolean;
    usesBusinessRules?: boolean;
    usesUserDto?: boolean;
    usesOneToOne?: boolean;
    usesOneToMany?: boolean;
    usesCascadeType?: boolean;
    usesFetchType?: boolean;
    usesDateHandler?: boolean;
    usesCollectors?: boolean;
    usesGeneratedValue?: boolean;
    usesEnumerated?: boolean;
    usesAggregateState?: boolean;
    customImports?: Set<string>;
};

export type EntityGenerationOptions = {
    projectName: string;
    allSharedDtos?: any[];
    dtoMappings?: any[];
};

export function generateEntityCode(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
    const opts = options || { projectName };
    const isRootEntity = entity.isRoot || false;
    const importReqs: ImportRequirements = {
        usesPersistence: true,
        usesLocalDateTime: false,
        usesBigDecimal: false,
        usesSet: false,
        usesList: false,
        usesAggregate: isRootEntity,
        usesStreams: false,
        usesBusinessRules: entity.rules && entity.rules.length > 0,
        customImports: new Set<string>()
    };

    const { code: fields, imports: fieldImports } = generateFields(entity.properties, entity, entity.isRoot || false, projectName);
    Object.assign(importReqs, fieldImports);

    const { code: defaultConstructor } = generateDefaultConstructor(entity.name);

    const { code: dtoConstructor, imports: dtoImports } = generateEntityDtoConstructor(entity, projectName, opts.allSharedDtos, opts.dtoMappings);
    Object.assign(importReqs, dtoImports);

    const { code: copyConstructor, imports: copyImports } = generateCopyConstructor(entity);
    Object.assign(importReqs, copyImports);

    // Generate getters/setters for regular properties
    const { code: gettersSetters } = generateGettersSetters(entity.properties, entity, projectName);

    // Generate getter/setter for back-reference field in non-root entities
    const backRefGetterSetter = (!isRootEntity && entity.$container)
        ? generateBackReferenceGetterSetter(entity.$container.name)
        : '';

    const { code: invariants, imports: invariantImports } = generateInvariants(entity);

    if (invariantImports) {
        Object.assign(importReqs, invariantImports);
    }

    const { code: constructors, imports: constructorImports } = generateConstructors(entity);

    if (constructorImports) {
        Object.assign(importReqs, constructorImports);
    }

    const { code: methods, imports: methodImports } = generateMethods(entity);

    if (methodImports) {
        Object.assign(importReqs, methodImports);
    }


    const aggregateName = entity.$container?.name || 'unknown';
    const imports = generateImports(importReqs, projectName, isRootEntity, aggregateName, entity.name);
    const extendsClause = isRootEntity
        ? (aggregateName !== entity.name ? ` extends ${aggregateName}` : ' extends Aggregate')
        : '';
    const abstractModifier = isRootEntity ? 'abstract ' : '';
    const config = getGlobalConfig();
    const packageName = config.buildPackageName(
        projectName,
        'microservices',
        aggregateName.toLowerCase(),
        'aggregate'
    );

    const jpaAnnotation = '@Entity';

    return `package ${packageName};

${imports}

${jpaAnnotation}
public ${abstractModifier}class ${entity.name}${extendsClause} {
${fields} 
${defaultConstructor}
${dtoConstructor}
${copyConstructor}
${constructors}
${gettersSetters}
${backRefGetterSetter}
${methods}
${invariants}
}`;
}

export function generateDtoCode(entity: Entity, projectName: string): string {
    const config = getGlobalConfig();
    const packageName = config.buildPackageName(
        projectName,
        'microservices',
        entity.$container?.name?.toLowerCase() || 'unknown',
        'aggregate'
    );
    const fields = generateDtoFields(entity);
    const constructor = generateDtoConstructor(entity);
    const gettersSetters = generateGettersSetters(entity.properties);

    const importReqs: ImportRequirements = {
        usesLocalDateTime: entity.properties.some((p: any) => resolveJavaType(p.type) === 'LocalDateTime'),
        usesSet: entity.properties.some((p: any) => resolveJavaType(p.type).startsWith('Set<')),
        customImports: new Set(['import java.io.Serializable;'])
    };

    return `package ${packageName};

import java.io.Serializable;
${importReqs.usesLocalDateTime ? 'import java.time.LocalDateTime;' : ''}
${importReqs.usesSet ? 'import java.util.Set;\nimport java.util.HashSet;' : ''}

public class ${entity.name}Dto implements Serializable {
${fields}
${constructor}
${gettersSetters}
}`;
}

function generateDtoFields(entity: Entity): string {
    const simpleFields = entity.properties.map((property: any) => {
        const javaType = TypeResolver.resolveJavaType(property.type);
        return `\tprivate ${javaType} ${property.name};`;
    }).join('\n');

    return simpleFields;
}

function generateDtoConstructor(entity: Entity): string {
    const params = entity.properties
        .map((p: any) => {
            return `${resolveJavaType(p.type)} ${p.name}`;
        })
        .join(', ');
    const assignments = entity.properties
        .map((p: any) => `\t\tthis.${p.name} = ${p.name};`)
        .join('\n');

    return `
\tpublic ${entity.name}Dto() {}

\tpublic ${entity.name}Dto(${params}) {
${assignments}
\t}`;
}

function generateFields(properties: any[], entity: Entity, isRootEntity: boolean, projectName: string): { code: string, imports: ImportRequirements } {
    const imports: ImportRequirements = {
        usesLocalDateTime: false,
        usesBigDecimal: false,
        usesSet: false,
        usesList: false,
        usesOneToOne: false,
        usesOneToMany: false,
        usesCascadeType: false,
        usesFetchType: false
    };

    let fields = properties.map((prop: any, index: number) => {
        const javaType = resolveJavaType(prop.type, prop.name);
        if (javaType.includes('LocalDateTime')) imports.usesLocalDateTime = true;
        if (javaType.includes('BigDecimal')) imports.usesBigDecimal = true;
        if (javaType.startsWith('Set<')) imports.usesSet = true;
        if (javaType.startsWith('List<')) imports.usesList = true;

        // Check if this is an enum type (by AST type or naming convention)
        if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            if (!imports.customImports) imports.customImports = new Set();
            imports.customImports.add(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'enums')}.${javaType};`);
            imports.usesEnumerated = true;
        }

        // Check if this is AggregateState type
        if (javaType === 'AggregateState') {
            imports.usesAggregateState = true;
        }

        // Add @Id annotation for the first field of any entity, and @GeneratedValue for non-root entities
        let idAnnotation = '';
        if (index === 0) {
            if (isRootEntity) {
                idAnnotation = '    @Id\n';
            } else {
                idAnnotation = '    @Id\n    @GeneratedValue\n';
                imports.usesGeneratedValue = true;
            }
        }

        // Determine if this is an entity relationship
        const isEntityType = TypeResolver.isEntityType(javaType) && !isEnumType(prop.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
        const isEntityCollection = isCollection && TypeResolver.isEntityType(TypeResolver.getElementType(prop.type) || '');

        let relationshipAnnotation = '';
        let initialization = '';

        if (isEntityType && !isCollection && !isEnumType(prop.type)) {
            // Single entity relationship - @OneToOne
            relationshipAnnotation = `    @OneToOne(cascade = CascadeType.ALL, mappedBy = "${entity.name.toLowerCase()}")\n`;
            imports.usesOneToOne = true;
            imports.usesCascadeType = true;
        } else if (isEntityCollection) {
            // Collection of entities - @OneToMany
            relationshipAnnotation = `    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "${entity.name.toLowerCase()}")\n`;
            initialization = ' = new HashSet<>()';
            imports.usesOneToMany = true;
            imports.usesCascadeType = true;
            imports.usesFetchType = true;
        }

        // Add @Enumerated annotation for enum types
        let enumAnnotation = '';
        if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            enumAnnotation = `    @Enumerated(EnumType.STRING)\n`;
        }

        return `${idAnnotation}${relationshipAnnotation}${enumAnnotation}    private ${javaType} ${prop.name}${initialization};`;
    }).join('\n');

    // Add back-reference field for non-root entities
    if (!isRootEntity && entity.$container) {
        const parentEntityName = entity.$container.name;
        const backRefField = `    @OneToOne\n    private ${parentEntityName} ${parentEntityName.toLowerCase()};`;
        fields = fields + '\n' + backRefField;
        imports.usesOneToOne = true;
    }

    return { code: fields, imports };
}


function generateDefaultConstructor(name: string): { code: string } {
    return {
        code: `\n    public ${name}() {\n    }`
    };
}

function generateEntityDtoConstructor(entity: Entity, projectName: string, allSharedDtos?: any[], dtoMappings?: any[]): { code: string, imports: ImportRequirements } {
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

    // Generate setter calls using DTO properties
    // For custom DTO types, we need to map entity fields to DTO fields
    const setterCalls = entity.properties.map((prop: any, index: number) => {
        // Skip the first property (id) for non-root entities as it's @GeneratedValue
        if (!isRootEntity && index === 0) {
            return '';
        }

        const javaType = resolveJavaType(prop.type);
        const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);

        // Determine the DTO getter method name
        let dtoGetterName = capitalizedName;

        // If using a custom DTO, map entity field names to DTO field names
        if (customDtoType) {
            // Apply reverse mapping: entity field -> DTO field
            // For ExecutionStudent fields like "studentAggregateId", "studentName", etc.
            // Map to UserDto fields like "id", "name", etc.
            const mappedName = mapEntityFieldToDtoField(prop.name, customDtoType, dtoMappings, entity);
            if (mappedName === null) {
                // Skip fields that don't exist in the custom DTO
                return '';
            }
            dtoGetterName = mappedName;
        }

        if (javaType === 'LocalDateTime') {
            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`;
        } else if (TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
            return `        set${capitalizedName}(${prop.name});`;
        } else if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            // For enum types, use valueOf to convert from String DTO field
            return `        set${capitalizedName}(${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}()));`;
        } else if (!javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`;
        }
        return ''; // Skip collections, they're initialized empty
    }).filter(call => call !== '').join('\n');

    // Helper function to map entity field names to DTO field names for custom DTOs
    // Returns null if the field doesn't exist in the DTO
    // Dynamically reads from DSL mapping blocks in shared-dtos.nebula
    function mapEntityFieldToDtoField(entityFieldName: string, dtoType: string, dtoMappings?: any[], entity?: Entity): string | null {
        if (!dtoMappings || !entity) {
            // Fallback: just capitalize (assuming the field exists in the DTO)
            return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
        }

        // Find the mapping for this entity's collection in the target DTO
        // For ExecutionStudent -> UserDto mapping via the 'students' collection
        const aggregateName = entity.$container?.name;
        const entityCollectionName = findCollectionNameForEntity(entity, aggregateName);

        if (!entityCollectionName) {
            // No collection mapping found, use default capitalization
            return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
        }

        // Find the DTO mapping block for this collection
        for (const mapping of dtoMappings) {
            // Check if this mapping is for the target DTO and collection
            if (mapping.targetDto?.ref?.name === dtoType &&
                mapping.collectionName?.toLowerCase() === entityCollectionName.toLowerCase()) {

                // Look for a field mapping for this entity field
                if (mapping.fieldMappings) {
                    for (const fieldMapping of mapping.fieldMappings) {
                        // The mapping is: dtoField -> entityField
                        // We need the reverse: entityField -> dtoField
                        if (fieldMapping.entityField === entityFieldName) {
                            // Found a mapping! Return the DTO field name (capitalized for getter)
                            return capitalize(fieldMapping.dtoField);
                        }
                    }
                }
            }
        }

        // No explicit mapping found - check if this field exists in the DTO definition
        // If not found in mappings, return null to skip it
        return null;
    }

    // Helper to find which collection this entity belongs to
    function findCollectionNameForEntity(entity: Entity, aggregateName?: string): string | null {
        if (!aggregateName || !entity.$container) {
            return null;
        }

        // Find the root entity in the aggregate
        const aggregate = entity.$container;
        const rootEntity = (aggregate as any).entities?.find((e: any) => e.isRoot);

        if (!rootEntity) {
            return null;
        }

        // Search through root entity's properties to find a collection of this entity type
        for (const prop of rootEntity.properties || []) {
            const propType = prop.type;

            // Check if this is a Set or List containing our entity type
            // Note: In the AST, collections are 'CollectionType' (Set or List)
            if (propType && (propType.$type === 'SetType' || propType.$type === 'ListType' || propType.$type === 'CollectionType')) {
                const elementType = propType.elementType;

                // Check if the element type references our entity
                if (elementType && elementType.$type === 'EntityType') {
                    const referencedEntityName = elementType.type?.ref?.name || elementType.type?.$refText;

                    if (referencedEntityName === entity.name) {
                        // Found the collection!
                        return prop.name;
                    }
                }
            }
        }

        // Fallback: use naming convention
        const pluralForm = entity.name.charAt(0).toLowerCase() + entity.name.slice(1) + 's';
        return pluralForm;
    }

    // Different constructor body for root vs non-root entities
    const constructorBody = isRootEntity ?
        `        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
${setterCalls}` :
        setterCalls;

    const code = `\n    public ${entityName}(${params}) {
${constructorBody}
    }`;

    // Add import for the DTO (either shared or local)
    const imports: ImportRequirements = {};
    // Check if we should use shared DTO for the root entity
    const dtoImportPath = getSharedDtoImportPath(dtoTypeName, projectName, allSharedDtos);
    if (dtoImportPath) {
        if (!imports.customImports) imports.customImports = new Set();
        imports.customImports.add(`import ${dtoImportPath};`);
    }

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

function generateCopyConstructor(entity: Entity): { code: string, imports: ImportRequirements } {
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

function generateGettersSetters(properties: any[], entity?: Entity, projectName?: string): { code: string } {
    const entityName = entity?.name || 'Unknown';

    const methods = properties.map((prop: any) => {
        const javaType = resolveJavaType(prop.type, prop.name);
        const capName = capitalize(prop.name);
        const getter = `get${capName}`;

        const getterMethod = `\n    public ${javaType} ${getter}() {\n        return ${prop.name};\n    }`;

        // Generate setter with bidirectional relationship handling
        const setterMethod = generateBidirectionalSetter(prop, javaType, capName, entityName);

        return `${getterMethod}\n\n${setterMethod}`;
    }).join('\n');

    return { code: methods };
}

function generateBidirectionalSetter(prop: any, javaType: string, capName: string, entityName: string): string {
    const propName = prop.name;
    const isEntityType = TypeResolver.isEntityType(javaType);
    const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

    if (isEntityType && !isCollection) {
        // Single entity relationship - set back-reference
        const backRefMethod = `set${entityName}`;

        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
        if (this.${propName} != null) {
            this.${propName}.${backRefMethod}(this);
        }
    }`;
    } else if (isCollection && TypeResolver.isEntityType(TypeResolver.getElementType(prop.type) || '')) {
        // Collection of entities - set back-reference for each element
        const elementType = TypeResolver.getElementType(prop.type);
        const backRefMethod = `set${entityName}`;

        if (elementType) {
            return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
        if (this.${propName} != null) {
            this.${propName}.forEach(${elementType.toLowerCase()} -> ${elementType.toLowerCase()}.${backRefMethod}(this));
        }
    }`;
        } else {
            // Fallback for undefined elementType
            return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
    }`;
        }
    } else {
        // Simple property - no bidirectional relationship
        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
    }`;
    }
}

function generateBackReferenceGetterSetter(parentEntityName: string): string {
    const fieldName = parentEntityName.toLowerCase();
    const capName = capitalize(parentEntityName);

    return `
    public ${parentEntityName} get${capName}() {
        return ${fieldName};
    }

    public void set${capName}(${parentEntityName} ${fieldName}) {
        this.${fieldName} = ${fieldName};
    }`;
}

function generateInvariants(entity: Entity): { code: string, imports?: ImportRequirements } {
    return { code: '', imports: undefined };
}

function generateImports(importReqs: ImportRequirements, projectName: string, isRoot: boolean, aggregateName?: string, entityName?: string): string {
    const imports: string[] = [];

    if (importReqs.usesPersistence) {
        imports.push('import jakarta.persistence.Entity;');
        imports.push('import jakarta.persistence.Id;');
    }
    if (importReqs.usesGeneratedValue) imports.push('import jakarta.persistence.GeneratedValue;');
    if (importReqs.usesOneToOne) imports.push('import jakarta.persistence.OneToOne;');
    if (importReqs.usesOneToMany) imports.push('import jakarta.persistence.OneToMany;');
    if (importReqs.usesCascadeType) imports.push('import jakarta.persistence.CascadeType;');
    if (importReqs.usesFetchType) imports.push('import jakarta.persistence.FetchType;');
    if (importReqs.usesEnumerated) imports.push('import jakarta.persistence.Enumerated;\nimport jakarta.persistence.EnumType;');
    if (importReqs.usesDateHandler) imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;');
    if (importReqs.usesCollectors) imports.push('import java.util.stream.Collectors;');
    if (importReqs.usesAggregateState) imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;');
    if (isRoot) {
        if (aggregateName && entityName && aggregateName !== entityName) {
        } else {
            imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;');
        }
        // Note: UnitOfWork and AggregateState imports are now dynamic based on actual usage
    }
    if (importReqs.usesLocalDateTime) imports.push('import java.time.LocalDateTime;');
    if (importReqs.usesBigDecimal) imports.push('import java.math.BigDecimal;');
    if (importReqs.usesSet) imports.push('import java.util.Set;\nimport java.util.HashSet;');
    if (importReqs.usesList) imports.push('import java.util.List;\nimport java.util.ArrayList;');
    if (importReqs.usesUserDto) imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', 'user', 'aggregate')}.UserDto;`);

    if (importReqs.customImports) {
        imports.push(...Array.from(importReqs.customImports));
    }

    return imports.join('\n');
}

export function generateConstructors(entity: Entity): { code: string, imports?: ImportRequirements } {
    if (!entity.constructors || entity.constructors.length === 0) {
        return { code: '' };
    }

    const importReqs: ImportRequirements = {};

    const constructorCode = entity.constructors.map((constructor: any) => {
        const params = constructor.parameters ? constructor.parameters.map((param: any) => {
            const paramType = resolveParamReturnType(param.type as any);
            if (paramType === 'UserDto') {
                importReqs.usesUserDto = true;
            }
            return `${paramType} ${param.name}`;
        }).join(', ') : '';

        const constructorBody = constructor.body ? constructor.body.replace(/"/g, '').split('\n').map((line: any) => `\t\t${line}`).join('\n') : '';

        return `\tpublic ${entity.name}(${params}) {
${constructorBody}
\t}`;
    }).join('\n\n');

    return {
        code: constructorCode,
        imports: importReqs
    };
}

export function generateMethods(entity: Entity): { code: string, imports?: ImportRequirements } {
    if (!entity.methods || entity.methods.length === 0) {
        return { code: '' };
    }

    const importReqs: ImportRequirements = {};

    const methodCode = entity.methods.map((method: any) => {
        const params = method.parameters ? method.parameters.map((param: any) => {
            const paramType = resolveParamReturnType(param.type as any);
            if (paramType === 'UserDto') {
                importReqs.usesUserDto = true;
            }
            return `${paramType} ${param.name}`;
        }).join(', ') : '';

        const returnType = method.returnType ? resolveParamReturnType(method.returnType as any) : 'void';
        if (returnType === 'UserDto') {
            importReqs.usesUserDto = true;
        }
        const methodBody = method.body ? method.body.replace(/"/g, '').split('\n').map((line: any) => `\t\t${line}`).join('\n') : '';

        const needsReturn = returnType !== 'void' && (!method.body || method.body.trim() === '');
        const defaultReturn = needsReturn ? `\t\treturn null; // TODO: Implement method` : '';

        return `\tpublic ${returnType} ${method.name}(${params}) {
${methodBody}${needsReturn ? '\n' + defaultReturn : ''}
\t}`;
    }).join('\n\n');

    return { code: methodCode, imports: importReqs };
}




export class EntityGenerator {
    async generateEntity(entity: Entity, options: EntityGenerationOptions): Promise<string> {
        return generateEntityCode(entity, options.projectName, options);
    }
}

