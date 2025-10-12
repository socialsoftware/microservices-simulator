import { Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { TypeResolver } from "../../../shared/resolvers/type-resolver.js";

const resolveJavaType = (type: any, fieldName?: string) => {
    return TypeResolver.resolveJavaType(type);
};

const toCamelCase = (str: string) => {
    return str.charAt(0).toLowerCase() + str.slice(1);
};

const detectPrimaryKey = (elementType: string, allEntities?: Entity[]): { pkType: string, pkGetter: string } => {
    if (allEntities) {
        const targetEntity = allEntities.find(e => e.name === elementType);
        if (targetEntity && targetEntity.properties.length > 0) {
            const firstProperty = targetEntity.properties[0];
            const pkType = resolveJavaType(firstProperty.type);
            const pkName = firstProperty.name;
            const pkGetter = `get${capitalize(pkName)}`;
            return { pkType, pkGetter };
        }
    }

    console.warn(`Warning: Could not determine primary key for entity '${elementType}'. Using default Long id.`);
    return { pkType: 'Long', pkGetter: 'getId' };
};

export function generateGettersSetters(properties: any[], entity?: Entity, projectName?: string, allEntities?: Entity[]): { code: string } {
    const entityName = entity?.name || 'Unknown';

    const methods = properties.map((prop: any) => {
        const javaType = resolveJavaType(prop.type, prop.name);
        const capName = capitalize(prop.name);
        const getter = `get${capName}`;

        const getterMethod = `\n    public ${javaType} ${getter}() {\n        return ${prop.name};\n    }`;

        // Generate setter with bidirectional relationship handling
        const setterMethod = generateBidirectionalSetter(prop, javaType, capName, entityName);

        // Generate collection management methods for collection properties
        const collectionMethods = generateCollectionMethods(prop, javaType, capName, entityName, allEntities);

        return `${getterMethod}\n\n${setterMethod}${collectionMethods}`;
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
        const backRefMethod = `set${entityName}`;

        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
        if (this.${propName} != null) {
            this.${propName}.forEach(item -> item.${backRefMethod}(this));
        }
    }`;
    } else {
        // Simple property - no bidirectional relationship
        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
    }`;
    }
}

function generateCollectionMethods(prop: any, javaType: string, capName: string, entityName: string, allEntities?: Entity[]): string {
    // Only generate collection methods for collection types (Set<> or List<>)
    if (!javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
        return '';
    }

    const elementType = TypeResolver.getElementType(prop.type);
    if (!elementType || !TypeResolver.isEntityType(elementType)) {
        // Only generate collection methods for entity collections, not primitive collections
        return '';
    }

    const propName = prop.name;
    const elementTypeCamelCase = toCamelCase(elementType);
    const backRefMethod = `set${entityName}`;

    // Determine collection type for initialization
    const collectionImpl = javaType.startsWith('List<') ? 'ArrayList' : 'HashSet';

    // Detect the primary key of the element type
    const { pkType, pkGetter } = detectPrimaryKey(elementType, allEntities);

    return `

    public void add${elementType}(${elementType} ${elementTypeCamelCase}) {
        if (this.${propName} == null) {
            this.${propName} = new ${collectionImpl}<>();
        }
        this.${propName}.add(${elementTypeCamelCase});
        if (${elementTypeCamelCase} != null) {
            ${elementTypeCamelCase}.${backRefMethod}(this);
        }
    }

    public void remove${elementType}(${pkType} id) {
        if (this.${propName} != null) {
            this.${propName}.removeIf(item -> 
                item.${pkGetter}() != null && item.${pkGetter}().equals(id));
        }
    }

    public boolean contains${elementType}(${pkType} id) {
        if (this.${propName} == null) {
            return false;
        }
        return this.${propName}.stream().anyMatch(item -> 
            item.${pkGetter}() != null && item.${pkGetter}().equals(id));
    }

    public ${elementType} find${elementType}ById(${pkType} id) {
        if (this.${propName} == null) {
            return null;
        }
        return this.${propName}.stream()
            .filter(item -> item.${pkGetter}() != null && item.${pkGetter}().equals(id))
            .findFirst()
            .orElse(null);
    }`;
}

export function generateBackReferenceGetterSetter(parentEntityName: string): string {
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