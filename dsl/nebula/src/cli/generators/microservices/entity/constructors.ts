import { Entity } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import { ImportRequirements } from "./types.js";

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

const isEnumTypeByNaming = (javaType: string) => {
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

    const finalFieldInitializations = entity.properties.map((prop: any, index: number) => {
        if (!isRootEntity && index === 0) {
            return '';
        }

        const isFinal = (prop as any).isFinal || false;
        if (!isFinal) {
            return '';
        }

        const javaType = resolveJavaType(prop.type);
        const defaultValue = (prop as any).defaultValue;

        const javaDefaultValue = convertDefaultValueToJava(defaultValue, javaType, prop);

        return `        this.${prop.name} = ${javaDefaultValue};`;
    }).filter(init => init !== '').join('\n');

    const constructorBody = finalFieldInitializations ? finalFieldInitializations : '';

    return {
        code: `\n    public ${entityName}() {\n${constructorBody}\n    }`
    };
}

export function generateEntityDtoConstructor(entity: Entity, projectName: string, allSharedDtos?: any[], dtoMappings?: any[]): { code: string, imports: ImportRequirements } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;

    const entityAny = entity as any;
    const dtoTypeRef = entityAny.dtoType;

    const customDtoType = dtoTypeRef?.ref?.name || dtoTypeRef?.$refText;

    let dtoName: string;
    let dtoTypeName: string;

    if (customDtoType) {
        dtoName = customDtoType;
        dtoTypeName = customDtoType;
    } else {
        const rootEntityName = isRootEntity ? entityName : (entity.$container?.name || entityName);
        dtoName = `${rootEntityName}Dto`;
        dtoTypeName = dtoName;
    }

    const entityRelationships = entity.properties.filter((prop: any) => {
        const javaType = resolveJavaType(prop.type);
        return TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<');
    });

    const relationshipParams = entityRelationships.map((prop: any) =>
        `${resolveJavaType(prop.type)} ${prop.name}`
    ).join(', ');

    const dtoParamName = customDtoType ?
        customDtoType.charAt(0).toLowerCase() + customDtoType.slice(1) :
        `${(isRootEntity ? entityName : (entity.$container?.name || entityName)).toLowerCase()}Dto`;

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

    const imports: ImportRequirements = {};
    if (!imports.customImports) imports.customImports = new Set();
    const dtoImportPath = `${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${dtoTypeName}`;
    imports.customImports.add(`import ${dtoImportPath};`);

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

    const setterCalls = entity.properties.map((prop: any, index: number) => {
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
                imports.usesCollectors = true; // Only set when actually using Collectors
                return `        set${capitalizedName}(other.get${capitalizedName}().stream().map(${elementType}::new).collect(Collectors.toSet()));`;
            } else {
                imports.usesSet = true; // Need HashSet import
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

function mapEntityFieldToDtoField(entityFieldName: string, dtoType: string, dtoMappings?: any[], entity?: Entity): string | null {
    const entityAny = entity as any;
    if (entityAny?.dtoMapping?.fieldMappings) {
        for (const fieldMapping of entityAny.dtoMapping.fieldMappings) {
            if (fieldMapping.entityField === entityFieldName) {
                return capitalize(fieldMapping.dtoField);
            }
        }
        return null;
    }

    if (!dtoMappings || !entity) {
        return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
    }

    const entityName = entity.name;
    const expectedDtoName = `${entityName}Dto`;
    if (dtoType === expectedDtoName) {
        return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
    }

    return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
}

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

    const entityAny = entity as any;
    if (entityAny?.dtoMapping?.fieldMappings) {
        const fieldMapping = entityAny.dtoMapping.fieldMappings.find((fm: any) =>
            fm.entityField === prop.name
        );

        if (fieldMapping && fieldMapping.extractField) {
            const extractField = fieldMapping.extractField;
            const extractMethod = `get${extractField.charAt(0).toUpperCase() + extractField.slice(1)}`;
            const collectorMethod = javaType.startsWith('List<') ? 'toList' : 'toSet';

            const sourceDtoType = fieldMapping.dtoField; // This should be the collection name
            const elementDtoType = inferDtoTypeFromCollection(sourceDtoType, customDtoType);

            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}().stream()
            .map(${elementDtoType}::${extractMethod})
            .collect(Collectors.${collectorMethod}()));`;
        }
    }

    return '';
}

function inferDtoTypeFromCollection(collectionName: string, baseDtoType: string): string {
    let singular: string;

    if (collectionName.endsWith('ies')) {
        singular = collectionName.slice(0, -3) + 'y';
    } else if (collectionName.endsWith('es') && collectionName.length > 3) {
        singular = collectionName.slice(0, -2);
    } else if (collectionName.endsWith('s') && collectionName.length > 1) {
        singular = collectionName.slice(0, -1);
    } else {
        singular = collectionName;
    }

    return `${singular.charAt(0).toUpperCase() + singular.slice(1)}Dto`;
}
