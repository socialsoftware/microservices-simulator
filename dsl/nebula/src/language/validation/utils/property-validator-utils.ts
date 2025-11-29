import type { Entity, Property } from "../../generated/ast.js";

export class PropertyValidatorUtils {
    static findPropertyInEntity(propertyName: string, entity: Entity, allEntities: Entity[]): Property | null {
        const camelCaseName = propertyName.charAt(0).toLowerCase() + propertyName.slice(1);

        if (entity.isRoot) {
            const aggregateProperties = ['aggregateId', 'id', 'version', 'state'];
            const propertyNameLower = propertyName.toLowerCase();
            if (aggregateProperties.some(prop => prop.toLowerCase() === propertyNameLower)) {
                return { name: propertyName } as Property;
            }

            if (propertyName.toLowerCase() === 'sagastate') {
                return { name: propertyName } as Property;
            }
        }

        const directMatch = entity.properties?.find((p: any) =>
            p.name.toLowerCase() === propertyName.toLowerCase() ||
            p.name.toLowerCase() === camelCaseName.toLowerCase()
        );
        if (directMatch) {
            return directMatch;
        }

        const camelCaseMatch = entity.properties?.find((p: any) => p.name === camelCaseName);
        if (camelCaseMatch) {
            return camelCaseMatch;
        }

        if (entity.properties && allEntities) {
            for (const prop of entity.properties) {
                const relationPascalCase = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                if (propertyName.startsWith(relationPascalCase)) {
                    const remaining = propertyName.substring(relationPascalCase.length);
                    if (remaining) {
                        const propType = (prop as any).type;
                        if (propType?.$type === 'EntityType' && propType.type) {
                            const relatedEntityName = propType.type.ref?.name || propType.type.$refText;
                            if (relatedEntityName) {
                                const relatedEntity = allEntities.find((e: any) => e.name === relatedEntityName);
                                if (relatedEntity) {
                                    const nestedProperty = this.findPropertyInEntity(remaining, relatedEntity, allEntities);
                                    if (nestedProperty) {
                                        return prop;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    static suggestPropertyNames(propertyName: string, entity: Entity, allEntities?: Entity[]): string[] {
        const suggestions: string[] = [];
        const propertyNameLower = propertyName.toLowerCase();

        if (entity.properties) {
            for (const prop of entity.properties) {
                const propNameLower = prop.name.toLowerCase();
                if (propNameLower.includes(propertyNameLower) || propertyNameLower.includes(propNameLower)) {
                    suggestions.push(prop.name);
                }
            }
        }

        return suggestions.slice(0, 3);
    }
}

