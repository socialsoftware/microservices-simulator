import type { Entity, Property } from "../../generated/ast.js";

/**
 * Get effective properties for an entity, including those defined in DTO mappings.
 * This is a local copy for the validation layer.
 */
function getEffectivePropertiesForValidation(entity: Entity): any[] {
    const explicitProps = entity.properties || [];
    const entityAny = entity as any;
    const mappings = entityAny.dtoMapping?.fieldMappings || [];

    // Get properties defined in mappings (new syntax with type)
    const mappingProps = mappings
        .filter((m: any) => m.type && m.entityField)
        .map((m: any) => ({
            name: m.entityField,
            type: m.type,
            $fromMapping: true
        }));

    // Combine, avoiding duplicates
    const explicitNames = new Set(explicitProps.map((p: any) => p.name));
    return [
        ...explicitProps,
        ...mappingProps.filter((p: any) => !explicitNames.has(p.name))
    ];
}

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

        // Use effective properties which includes mapping-defined properties
        const effectiveProps = getEffectivePropertiesForValidation(entity);

        const directMatch = effectiveProps.find((p: any) =>
            p.name.toLowerCase() === propertyName.toLowerCase() ||
            p.name.toLowerCase() === camelCaseName.toLowerCase()
        );
        if (directMatch) {
            return directMatch as Property;
        }

        const camelCaseMatch = effectiveProps.find((p: any) => p.name === camelCaseName);
        if (camelCaseMatch) {
            return camelCaseMatch as Property;
        }

        if (effectiveProps && allEntities) {
            for (const prop of effectiveProps) {
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

        const effectiveProps = getEffectivePropertiesForValidation(entity);
        for (const prop of effectiveProps) {
            const propNameLower = prop.name.toLowerCase();
            if (propNameLower.includes(propertyNameLower) || propertyNameLower.includes(propNameLower)) {
                suggestions.push(prop.name);
            }
        }

        return suggestions.slice(0, 3);
    }
}

