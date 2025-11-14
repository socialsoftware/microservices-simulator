import { Entity } from "../../../../language/generated/ast.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import { ImportRequirements } from "./types.js";
import type { DtoSchemaRegistry } from "../../../services/dto-schema-service.js";

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

export function generateEntityDtoConstructor(entity: Entity, projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry): { code: string, imports: ImportRequirements } {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;

    const entityAny = entity as any;
    const dtoTypeRef = entityAny.dtoType as string | undefined;

    const customDtoType = dtoTypeRef;

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

    const dtoSchema = dtoSchemaRegistry?.dtoByName?.[dtoTypeName];
    if (!dtoSchema) {
        throw new Error(`DTO schema for ${dtoTypeName} was not found. Ensure a root entity defines this DTO.`);
    }
    const dtoParamName = customDtoType ?
        customDtoType.charAt(0).toLowerCase() + customDtoType.slice(1) :
        `${(isRootEntity ? entityName : (entity.$container?.name || entityName)).toLowerCase()}Dto`;

    const entityRelationships: string[] = [];
    if (isRootEntity) {
        for (const prop of entity.properties || []) {
            const javaType = resolveJavaType(prop.type);
            if (TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
                entityRelationships.push(`${javaType} ${prop.name}`);
            }
        }
    }

    const relationshipParams = entityRelationships.join(', ');
    const params = isRootEntity ?
        (relationshipParams ?
            `Integer aggregateId, ${dtoTypeName} ${dtoParamName}, ${relationshipParams}` :
            `Integer aggregateId, ${dtoTypeName} ${dtoParamName}`) :
        `${dtoTypeName} ${dtoParamName}`;

    const setterCalls: string[] = [];

    for (const field of dtoSchema.fields) {
        if (field.isAggregateField) {
            continue;
        }

        const entityProp = field.sourceProperty;
        if (!entityProp) {
            continue;
        }

        const propName = entityProp.name;
        const capitalizedName = propName.charAt(0).toUpperCase() + propName.slice(1);
        const isFinal = (entityProp as any).isFinal || false;
        const dtoGetterName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const javaType = resolveJavaType(entityProp.type);
        const isEntityRelationship = TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<');

        if (isRootEntity && (field.requiresConversion && field.referencedEntityName)) {
            continue;
        }

        if (isFinal) {
            if (field.requiresConversion && field.referencedEntityName) {
                setterCalls.push(`        this.${propName} = ${dtoParamName}.get${dtoGetterName}() != null ? new ${field.referencedEntityName}(${dtoParamName}.get${dtoGetterName}()) : null;`);
            } else if (isEnumType(entityProp.type)) {
                setterCalls.push(`        this.${propName} = ${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}());`);
            } else {
                setterCalls.push(`        this.${propName} = ${dtoParamName}.get${dtoGetterName}();`);
            }
            continue;
        }

        if (field.requiresConversion) {
            if (field.isCollection && field.referencedEntityName) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                if (field.extractField) {
                    const extractMethod = `get${field.extractField.charAt(0).toUpperCase() + field.extractField.slice(1)}()`;
                    setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? ${dtoParamName}.get${dtoGetterName}().stream().map(dto -> dto.${extractMethod}).collect(${collector}) : null);`);
                } else {
                    setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? ${dtoParamName}.get${dtoGetterName}().stream().map(dto -> new ${field.referencedEntityName}(dto)).collect(${collector}) : null);`);
                }
            } else if (!field.isCollection && field.referencedEntityName) {
                setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? new ${field.referencedEntityName}(${dtoParamName}.get${dtoGetterName}()) : null);`);
            } else {
                setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`);
            }
            continue;
        }

        if (isEnumType(entityProp.type)) {
            setterCalls.push(`        set${capitalizedName}(${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}()));`);
            continue;
        }

        if (!isRootEntity && isEntityRelationship && field.referencedEntityName) {
            setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}() != null ? new ${field.referencedEntityName}(${dtoParamName}.get${dtoGetterName}()) : null);`);
            continue;
        }

        setterCalls.push(`        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`);
    }

    if (isRootEntity && entityRelationships.length > 0) {
        for (const prop of entity.properties || []) {
            const javaType = resolveJavaType(prop.type);
            if (TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
                const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                const isFinalProp = (prop as any).isFinal || false;
                if (isFinalProp) {
                    setterCalls.push(`        this.${prop.name} = ${prop.name};`);
                } else {
                    setterCalls.push(`        set${capitalizedName}(${prop.name});`);
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
    entity: Entity,
    projectName: string,
    dtoSchemaRegistry: DtoSchemaRegistry | undefined,
    imports: ImportRequirements
): void {
    if (!imports.customImports) {
        imports.customImports = new Set<string>();
    }

    const config = getGlobalConfig();
    const owningAggregateName = entity.$container?.name || entity.name;
    let targetAggregateName = owningAggregateName;

    const dtoSchema = dtoSchemaRegistry?.dtoByName?.[dtoTypeName];
    if (dtoSchema) {
        targetAggregateName = dtoSchema.aggregateName;
    } else if (dtoTypeName.endsWith('Dto')) {
        targetAggregateName = dtoTypeName.slice(0, -3);
    }

    if (!targetAggregateName) {
        return;
    }

    if (targetAggregateName.toLowerCase() === (owningAggregateName || '').toLowerCase()) {
        return;
    }

    const importPath = `${config.buildPackageName(projectName, 'microservices', targetAggregateName.toLowerCase(), 'aggregate')}.${dtoTypeName}`;
    imports.customImports.add(`import ${importPath};`);
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
