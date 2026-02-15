import { EntityExt, AggregateExt } from "../../../../types/ast-extensions.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { CrudHelpers } from "../../../common/crud-helpers.js";



export class EntityRelationshipExtractor {
    

    static findEntityRelationshipsWithDetails(
        rootEntity: EntityExt,
        aggregate: AggregateExt
    ): Array<{
        paramName: string;
        entityName: string;
        isCollection: boolean;
        collectionType: string;
        aggregateRef: string | null;
    }> {
        const relationships: Array<{
            paramName: string;
            entityName: string;
            isCollection: boolean;
            collectionType: string;
            aggregateRef: string | null;
        }> = [];

        if (!rootEntity.properties) {
            return relationships;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const collectionType = javaType.startsWith('Set<') ? 'Set' : 'List';

            
            const isEntityType = !CrudHelpers.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                
                let entityName: string;
                if (isCollection) {
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    const entityRef = (prop.type as any).type?.ref;
                    entityName = entityRef?.name || javaType;
                }

                
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                if (relatedEntity) {
                    
                    const aggregateRef = (relatedEntity as any).aggregateRef as string | undefined;

                    relationships.push({
                        paramName: prop.name,
                        entityName: entityName,
                        isCollection,
                        collectionType,
                        aggregateRef: aggregateRef || null
                    });
                }
            }
        }

        return relationships;
    }
}
