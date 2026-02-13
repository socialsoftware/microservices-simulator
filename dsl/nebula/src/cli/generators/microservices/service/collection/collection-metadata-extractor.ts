import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";

/**
 * Collection property metadata
 */
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

/**
 * Collection Metadata Extractor
 *
 * Responsible for extracting and analyzing collection properties from entities.
 * Identifies collections, determines business keys, and provides metadata
 * for collection operation generation.
 */
export class CollectionMetadataExtractor {

    /**
     * Find all collection properties in a root entity.
     *
     * Identifies Set<T> and List<T> properties where T is an entity type.
     * Extracts metadata needed for generating collection manipulation methods.
     *
     * @param rootEntity The root entity to analyze
     * @param aggregate The containing aggregate
     * @returns Array of collection property metadata
     */
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

    /**
     * Build aggregate ID field name for a projection entity.
     *
     * Extracts the referenced aggregate name and constructs the
     * aggregateId field name (e.g., "ExecutionUser" → "userAggregateId").
     *
     * @param entityName The projection entity name
     * @returns Aggregate ID field name
     */
    static buildAggregateIdFieldName(entityName: string): string {
        const referencedName = this.extractReferencedAggregateName(entityName);
        return `${referencedName.toLowerCase()}AggregateId`;
    }

    /**
     * Extract referenced aggregate name from projection entity name.
     *
     * Uses heuristic to extract the source aggregate name from a projection entity.
     * Example: "ExecutionUser" → "User", "TournamentCreator" → "Creator"
     *
     * Strategy: Find the last uppercase letter sequence (assumes camelCase naming).
     *
     * @param entityName The projection entity name
     * @returns Referenced aggregate name
     */
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

    /**
     * Determine business key field for an entity.
     *
     * Searches for common business key field names in order of preference:
     * 1. Common keys: 'key', 'code', 'id', 'sequence'
     * 2. First Integer field (excluding aggregateId, version)
     * 3. Fallback: 'key'
     *
     * @param entity The entity to analyze
     * @returns Business key field name
     */
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

    /**
     * Convert a word to singular form (simple heuristic).
     *
     * Removes trailing 's' if present. This is a simple heuristic
     * and may not work for all English plural forms.
     *
     * @param word The word to singularize
     * @returns Singular form
     */
    static singularize(word: string): string {
        if (word.endsWith('s')) {
            return word.slice(0, -1);
        }
        return word;
    }
}
