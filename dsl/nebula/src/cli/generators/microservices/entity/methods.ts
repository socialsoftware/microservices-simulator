import { Entity } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";

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

function propertyTypeMatchesEntity(prop: any, entityName: string, allEntities?: Entity[]): boolean {
    const propType = TypeResolver.resolveJavaType(prop.type);
    const propElementType = TypeResolver.getElementType(prop.type);

    if (propType === entityName || propElementType === entityName) {
        return true;
    }

    if (prop.type && typeof prop.type === 'object') {
        if (prop.type.$type === 'EntityType' && prop.type.type) {
            const ref: any = prop.type.type.ref;
            if (ref) {
                if (ref.$type === 'Entity' && ref.name === entityName) {
                    return true;
                }
                if (ref.name === entityName) {
                    return true;
                }
                if (allEntities) {
                    const referencedEntity = allEntities.find(e => e === ref);
                    if (referencedEntity && referencedEntity.name === entityName) {
                        return true;
                    }
                }
            }
            if (prop.type.type.$refText === entityName) {
                return true;
            }
        } else if ((prop.type.$type === 'ListType' || prop.type.$type === 'SetType' || prop.type.$type === 'CollectionType') && prop.type.elementType) {
            const elementRef: any = prop.type.elementType.ref;
            if (elementRef) {
                if (elementRef.$type === 'Entity' && elementRef.name === entityName) {
                    return true;
                }
                if (elementRef.name === entityName) {
                    return true;
                }
                if (allEntities) {
                    const referencedEntity = allEntities.find(e => e === elementRef);
                    if (referencedEntity && referencedEntity.name === entityName) {
                        return true;
                    }
                }
            }
            if (prop.type.elementType.$refText === entityName) {
                return true;
            }
        }
    }

    return false;
}

export function generateGettersSetters(properties: any[], entity?: Entity, projectName?: string, allEntities?: Entity[]): { code: string } {
    const entityName = entity?.name || 'Unknown';

    const methods = properties.map((prop: any) => {
        const javaType = resolveJavaType(prop.type, prop.name);
        const capName = capitalize(prop.name);
        const getter = `get${capName}`;
        const isFinal = (prop as any).isFinal || false;

        const getterMethod = `\n    public ${javaType} ${getter}() {\n        return ${prop.name};\n    }`;

        const setterMethod = isFinal ? '' : generateBidirectionalSetter(prop, javaType, capName, entityName, allEntities);

        const collectionMethods = generateCollectionMethods(prop, javaType, capName, entityName, allEntities);

        return `${getterMethod}${setterMethod ? '\n\n' + setterMethod : ''}${collectionMethods}`;
    }).join('\n');

    return { code: methods };
}

function generateBidirectionalSetter(prop: any, javaType: string, capName: string, entityName: string, allEntities?: Entity[]): string {
    const propName = prop.name;
    const isEntityType = TypeResolver.isEntityType(javaType);
    const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

    const findBackRefFieldName = (targetEntityType: string): string | null => {
        if (!allEntities) return null;
        const targetEntity = allEntities.find(e => e.name === targetEntityType);
        if (!targetEntity) return null;

        const backRefProp = targetEntity.properties.find((p: any) => {
            if (propertyTypeMatchesEntity(p, entityName, allEntities)) {
                return true;
            }
            const resolvedType = TypeResolver.resolveJavaType(p.type);
            const elementType = TypeResolver.getElementType(p.type);
            return resolvedType === entityName || elementType === entityName;
        });

        if (backRefProp) {
            const backRefCapName = capitalize(backRefProp.name);
            return `set${backRefCapName}`;
        }

        return null;
    };

    if (isEntityType && !isCollection) {
        const backRefMethod = findBackRefFieldName(javaType) || `set${entityName}`;

        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
        if (this.${propName} != null) {
            this.${propName}.${backRefMethod}(this);
        }
    }`;
    } else if (isCollection && TypeResolver.isEntityType(TypeResolver.getElementType(prop.type) || '')) {
        const elementType = TypeResolver.getElementType(prop.type) || '';
        const backRefMethod = findBackRefFieldName(elementType) || `set${entityName}`;

        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
        if (this.${propName} != null) {
            this.${propName}.forEach(item -> item.${backRefMethod}(this));
        }
    }`;
    } else {
        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
    }`;
    }
}

function generateCollectionMethods(prop: any, javaType: string, capName: string, entityName: string, allEntities?: Entity[]): string {
    if (!javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
        return '';
    }

    const elementType = TypeResolver.getElementType(prop.type);
    if (!elementType || !TypeResolver.isEntityType(elementType)) {
        return '';
    }

    const findBackRefFieldName = (targetEntityType: string): string | null => {
        if (!allEntities) return null;
        const targetEntity = allEntities.find(e => e.name === targetEntityType);
        if (!targetEntity) return null;

        const backRefProp = targetEntity.properties.find((p: any) => {
            if (propertyTypeMatchesEntity(p, entityName, allEntities)) {
                return true;
            }
            const resolvedType = TypeResolver.resolveJavaType(p.type);
            const elementType = TypeResolver.getElementType(p.type);
            return resolvedType === entityName || elementType === entityName;
        });

        if (backRefProp) {
            const backRefCapName = capitalize(backRefProp.name);
            return `set${backRefCapName}`;
        }

        return null;
    };

    const propName = prop.name;
    const elementTypeCamelCase = toCamelCase(elementType);
    const backRefMethod = findBackRefFieldName(elementType) || `set${entityName}`;

    const collectionImpl = javaType.startsWith('List<') ? 'ArrayList' : 'HashSet';

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