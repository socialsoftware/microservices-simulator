import { Entity, Property } from "../../language/generated/ast.js";
import { capitalize, resolveJavaType, isCollectionType, isEntityType } from "./utils.js";
import { ImportRequirements } from "./entity-generator.js";


export function generateFields(properties: Property[], entity: Entity): { code: string, imports: ImportRequirements } {
    const importReqs: ImportRequirements = {};

    const fieldsCode = properties.map(property => {
        const javaType = resolveJavaType(property.type);
        let fieldInfo;

        if (property.isKey || property.name === 'id') {
            fieldInfo = generateKeyField(javaType, property.name);
            importReqs.usesPersistence = true;
        } else if (isCollectionType(property.type)) {
            fieldInfo = generateCollectionField(javaType, property.name, property.type, entity);
            importReqs.usesSet = javaType.startsWith('Set');
            importReqs.usesList = javaType.startsWith('List');

            if (isEntityType(property.type.elementType)) {
                if (fieldInfo.isOneToMany || fieldInfo.isManyToMany) importReqs.usesPersistence = true;
            } else {
                importReqs.usesPersistence = true;
            }
        } else if (isEntityType(property.type)) {
            fieldInfo = generateEntityField(javaType, property.name, property.type, entity);
            if (fieldInfo.isManyToOne || fieldInfo.isOneToOne) importReqs.usesPersistence = true;
        } else {
            if (javaType === 'Aggregate.AggregateState') { importReqs.usesAggregate = true; }
            fieldInfo = generateSimpleField(javaType, property.name);

            // Check for specific data types
            if (javaType === 'LocalDateTime') importReqs.usesLocalDateTime = true;
            if (javaType === 'BigDecimal') importReqs.usesBigDecimal = true;
        }

        return fieldInfo.code;
    }).join('\n');

    return {
        code: fieldsCode,
        imports: importReqs
    };
}

function generateKeyField(javaType: string, name: string): { code: string, isId: boolean } {
    return {
        code: `\t@Id\n\t@GeneratedValue\n\tprivate ${javaType} ${name};`,
        isId: true
    };
}
function generateCollectionField(javaType: string, name: string, type: any, entity: Entity): {
    code: string,
    isOneToMany: boolean,
    isManyToMany: boolean
} {
    if (isEntityType(type.elementType)) {
        const referencedEntity = type.elementType.type.ref;
        if (!referencedEntity) {
            return {
                code: `\tprivate ${javaType} ${name} = new HashSet<>();`,
                isOneToMany: false,
                isManyToMany: false
            };
        }


        const hasCollectionPropertyToThisEntity = referencedEntity.properties.some(
            (p: Property) => isCollectionType(p.type) &&
                isEntityType(p.type.elementType) &&
                p.type.elementType.type.ref?.name === entity.name
        );

        if (hasCollectionPropertyToThisEntity) {
            return {
                code: `\n\t@ManyToMany\n\tprivate ${javaType} ${name} = new HashSet<>();`,
                isOneToMany: false,
                isManyToMany: true
            };
        } else {
            // Find the property in the referenced entity that points back to this entity
            const backRefProperty = referencedEntity.properties.find(
                (p: Property) => isEntityType(p.type) && p.type.type.ref?.name === entity.name
            );

            if (backRefProperty) {
                return {
                    code: `\n\t@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "${backRefProperty.name}")\n\tprivate ${javaType} ${name} = new HashSet<>();`,
                    isOneToMany: true,
                    isManyToMany: false
                };
            } else {
                return {
                    code: `\n\t@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)\n\tprivate ${javaType} ${name} = new HashSet<>();`,
                    isOneToMany: true,
                    isManyToMany: false
                };
            }
        }
    }
    return {
        code: `\tprivate ${javaType} ${name} = new HashSet<>();`,
        isOneToMany: false,
        isManyToMany: false
    };
}

function generateEntityField(javaType: string, name: string, type: any, entity: Entity): {
    code: string,
    isManyToOne: boolean,
    isOneToOne: boolean
} {
    const referencedEntity = type.type.ref;
    if (!referencedEntity) {
        return {
            code: `\tprivate ${javaType} ${name};`,
            isManyToOne: false,
            isOneToOne: false
        };
    }

    const hasCollectionPropertyToThisEntity = referencedEntity.properties.some(
        (p: Property) => isCollectionType(p.type) &&
            isEntityType(p.type.elementType) &&
            p.type.elementType.type.ref?.name === entity.name
    );

    if (hasCollectionPropertyToThisEntity) {
        return {
            code: `\n\t@ManyToOne\n\tprivate ${javaType} ${name};`,
            isManyToOne: true,
            isOneToOne: false
        };
    } else {
        // Find the collection property in this entity that points to the referenced entity
        

        const backRefProperty = referencedEntity.properties.find(
            (p: Property) => isEntityType(p.type) && p.type.type.ref?.name === entity.name
        );

        if (backRefProperty) {
            return {
                code: `\n\t@OneToOne(cascade = CascadeType.ALL, mappedBy = "${backRefProperty.name}")\n\tprivate ${javaType} ${name};`,
                isManyToOne: false,
                isOneToOne: true
            };
        } else {
            return {
                code: `\n\t@OneToOne(cascade = CascadeType.ALL)\n\tprivate ${javaType} ${name};`,
                isManyToOne: false,
                isOneToOne: true
            };
        }
    }
}

function generateSimpleField(javaType: string, name: string): { code: string } {
    return {
        code: `\tprivate ${javaType} ${name};`
    };
}

export function generateConstructor(entityName: string, properties: Property[]): { code: string } {
    const params = properties
        .filter(p => !p.isKey)
        .map(p => `${resolveJavaType(p.type)} ${p.name}`)
        .join(', ');
    const assignments = properties
        .filter(p => !p.isKey)
        .map(p => `\t\tthis.${p.name} = ${p.name};`)
        .join('\n');

    return {
        code: `
\tpublic ${entityName}() {}

\tpublic ${entityName}(${params}) {
${assignments}
\t}`
    };
}

export function generateCopyConstructor(entity: Entity): { code: string, imports: ImportRequirements } {
    const importReqs: ImportRequirements = {};

    const assignments = entity.properties
        .filter(p => !p.isKey)
        .map(p => {
            // Skip generating setters for root entity references
            if (isEntityType(p.type) && p.type.type.ref?.isRoot) {
                return null;
            }

            if (isCollectionType(p.type)) {
                if (isEntityType(p.type.elementType)) {
                    const elementType = p.type.elementType.type.ref?.name || 'Object';
                    importReqs.usesStreams = true;
                    return `\t\tset${capitalize(p.name)}(other.get${capitalize(p.name)}().stream().map(${elementType}::new).collect(Collectors.toSet()));`;
                } else {
                    return `\t\tset${capitalize(p.name)}(other.get${capitalize(p.name)}());`;
                }
            } else if (isEntityType(p.type)) {
                const entityType = p.type.type.ref?.name || 'Object';
                return `\t\tset${capitalize(p.name)}(new ${entityType}(other.get${capitalize(p.name)}()));`;
            } else {
                return `\t\tset${capitalize(p.name)}(other.get${capitalize(p.name)}());`;
            }
        })
        .filter(Boolean) // Remove null entries (skipped root entity references)
        .join('\n');

    const superCall = entity.isRoot ? '\t\tsuper(other);\n' : '';

    return {
        code: `
\tpublic ${entity.name}(${entity.name} other) {
${superCall}${assignments}
\t}`,
        imports: importReqs
    };
}

export function generateGettersSetters(properties: Property[]): { code: string } {
    return {
        code: properties.map(property => {
            const javaType = resolveJavaType(property.type);
            const capitalizedName = capitalize(property.name);
            return `
\tpublic ${javaType} get${capitalizedName}() {
\t\treturn ${property.name};
\t}

\tpublic void set${capitalizedName}(${javaType} ${property.name}) {
\t\tthis.${property.name} = ${property.name};
\t}`;
        }).join('\n')
    };
} 