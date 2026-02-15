import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../common/unified-type-resolver.js";
import { capitalize } from "../../../utils/generator-utils.js";

export interface CollectionMetadata {
    propertyName: string;           
    elementType: string;            
    elementDtoType: string;         
    identifierField: string;        
    identifierType: string;         
    isProjection: boolean;          
    collectionType: 'Set' | 'List';
    singularName: string;           
    capitalizedSingular: string;    
    capitalizedCollection: string;  
}

export class CollectionMetadataBuilder {
    

    static extractCollections(aggregate: Aggregate, rootEntity: Entity): CollectionMetadata[] {
        const metadata: CollectionMetadata[] = [];

        if (!rootEntity.properties) {
            return metadata;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isSet = javaType.startsWith('Set<');
            const isList = javaType.startsWith('List<');

            if (isSet || isList) {
                const elementType = TypeResolver.getElementType(prop.type);
                if (elementType && TypeResolver.isEntityType(javaType)) {
                    const collectionInfo = this.buildCollectionMetadata(
                        prop.name,
                        elementType,
                        isSet ? 'Set' : 'List',
                        aggregate
                    );
                    metadata.push(collectionInfo);
                }
            }
        }

        return metadata;
    }

    

    private static buildCollectionMetadata(
        propertyName: string,
        elementType: string,
        collectionType: 'Set' | 'List',
        aggregate: Aggregate
    ): CollectionMetadata {
        const elementEntity = this.findEntityByName(aggregate, elementType);
        const isProjection = this.hasAggregateRef(elementEntity);

        
        const identifierField = isProjection
            ? this.buildAggregateIdFieldName(elementType)
            : this.determineBusinessKey(elementEntity);

        const singularName = this.singularize(propertyName);

        return {
            propertyName,
            elementType,
            elementDtoType: `${elementType}Dto`,
            identifierField,
            identifierType: 'Integer',
            isProjection,
            collectionType,
            singularName,
            capitalizedSingular: capitalize(singularName),
            capitalizedCollection: capitalize(propertyName)
        };
    }

    

    private static hasAggregateRef(entity: Entity | null): boolean {
        if (!entity) return false;
        return (entity as any).aggregateRef !== undefined && (entity as any).aggregateRef !== null;
    }

    

    private static findEntityByName(aggregate: Aggregate, entityName: string): Entity | null {
        return aggregate.entities?.find((e: any) => e.name === entityName) || null;
    }

    

    private static buildAggregateIdFieldName(entityName: string): string {
        const referencedName = this.extractReferencedAggregateName(entityName);
        return `${referencedName.toLowerCase()}AggregateId`;
    }

    

    private static extractReferencedAggregateName(entityName: string): string {
        
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

    

    private static determineBusinessKey(entity: Entity | null): string {
        if (!entity || !entity.properties) {
            return 'key';
        }

        
        const commonKeys = ['key', 'code', 'id', 'sequence'];

        for (const keyName of commonKeys) {
            const field = entity.properties.find(p => p.name === keyName);
            if (field) {
                return keyName;
            }
        }

        
        const firstIntField = entity.properties.find(p => {
            const javaType = TypeResolver.resolveJavaType(p.type);
            return javaType === 'Integer' &&
                !p.name.endsWith('AggregateId') &&
                !p.name.endsWith('Version');
        });

        return firstIntField?.name || 'key';
    }

    

    private static singularize(word: string): string {
        if (word.endsWith('s')) {
            return word.slice(0, -1);
        }
        return word;
    }
}
