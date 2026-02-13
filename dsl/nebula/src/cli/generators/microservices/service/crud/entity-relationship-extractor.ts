import { EntityExt, AggregateExt } from "../../../../types/ast-extensions.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { CrudHelpers } from "../../../common/crud-helpers.js";

/**
 * Extracts relationship information from entity properties.
 *
 * Responsibilities:
 * - Identify entity relationships within an aggregate
 * - Determine if relationships are collections or single references
 * - Extract aggregate reference information
 * - Filter out non-entity types (enums, primitives)
 */
export class EntityRelationshipExtractor {
    /**
     * Find all entity relationships in the root entity with detailed metadata.
     *
     * Returns information about:
     * - Entity collections (Set<Entity>, List<Entity>)
     * - Single entity references
     * - Cross-aggregate references (entities with aggregateRef)
     *
     * @param rootEntity The root entity to analyze
     * @param aggregate The containing aggregate
     * @returns Array of relationship details
     */
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

            // Check if this is an entity type (not enum)
            const isEntityType = !CrudHelpers.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                // Get element type for collections
                let entityName: string;
                if (isCollection) {
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    const entityRef = (prop.type as any).type?.ref;
                    entityName = entityRef?.name || javaType;
                }

                // Find the related entity to check for aggregateRef
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                if (relatedEntity) {
                    // Check if this entity has an aggregateRef (references another aggregate)
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
