import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { capitalize } from "../../../../utils/generator-utils.js";



export interface CollectionProperty {
    propertyName: string;
    elementType: string;
    collectionType: 'Set' | 'List';
    isProjection: boolean;
    identifierField: string;
    singularName: string;
    capitalizedSingular: string;
    capitalizedCollection: string;
}



export class CollectionMetadataExtractor {

    

    static findCollectionProperties(rootEntity: Entity, aggregate: Aggregate): CollectionProperty[] {
        const collections: CollectionProperty[] = [];

        if (!rootEntity.properties) {
            return collections;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isSet = javaType.startsWith('Set<');
            const isList = javaType.startsWith('List<');

            if (isSet || isList) {
                const elementType = TypeResolver.getElementType(prop.type);
                if (elementType && TypeResolver.isEntityType(javaType)) {
                    const elementEntity = aggregate.entities?.find((e: any) => e.name === elementType);
                    if (!elementEntity) continue;

                    const isProjection = (elementEntity as any).aggregateRef !== undefined;
                    const identifierField = isProjection
                        ? this.buildAggregateIdFieldName(elementType)
                        : this.determineBusinessKey(elementEntity);

                    const singularName = this.singularize(elementType);

                    collections.push({
                        propertyName: prop.name,
                        elementType,
                        collectionType: isSet ? 'Set' : 'List',
                        isProjection,
                        identifierField,
                        singularName,
                        capitalizedSingular: capitalize(singularName),
                        capitalizedCollection: capitalize(prop.name)
                    });
                }
            }
        }

        return collections;
    }

    

    static buildAggregateIdFieldName(entityName: string): string {
        const referencedName = this.extractReferencedAggregateName(entityName);
        return `${referencedName.toLowerCase()}AggregateId`;
    }

    

    static extractReferencedAggregateName(entityName: string): string {
        for (let i = entityName.length - 1; i >= 0; i--) {
            if (entityName[i] === entityName[i].toUpperCase() && i > 0) {
                const candidate = entityName.substring(i);
                if (candidate.length > 1) {
                    return candidate;
                }
            }
        }
        return entityName;
    }

    

    static determineBusinessKey(entity: any): string {
        if (!entity || !entity.properties) {
            return 'key';
        }

        const commonKeys = ['key', 'code', 'id', 'sequence'];
        for (const keyName of commonKeys) {
            const field = entity.properties.find((p: any) => p.name === keyName);
            if (field) {
                return keyName;
            }
        }

        const firstIntField = entity.properties.find((p: any) => {
            const javaType = TypeResolver.resolveJavaType(p.type);
            return javaType === 'Integer' &&
                !p.name.endsWith('AggregateId') &&
                !p.name.endsWith('Version');
        });

        return firstIntField?.name || 'key';
    }

    

    static singularize(word: string): string {
        if (word.endsWith('s')) {
            return word.slice(0, -1);
        }
        return word;
    }
}
