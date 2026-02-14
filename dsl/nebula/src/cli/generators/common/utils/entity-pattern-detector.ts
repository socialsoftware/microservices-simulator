import { Aggregate } from "../../../../language/generated/ast.js";
import { getEntities } from "../../../utils/aggregate-helpers.js";

/**
 * Utility for dynamically detecting entity references in generated code.
 *
 * Replaces hardcoded entity name patterns with dynamic detection based on
 * the actual entities defined in the aggregates.
 */
export class EntityPatternDetector {
    /**
     * Builds a regex pattern that matches entity names from all aggregates.
     *
     * @param allAggregates All aggregates in the system
     * @returns RegExp that matches entity names
     */
    static buildEntityPattern(allAggregates: Aggregate[]): RegExp {
        const entityNames = allAggregates
            .flatMap(agg => getEntities(agg).map(e => e.name))
            .filter((name, index, self) => self.indexOf(name) === index); // Unique names

        if (entityNames.length === 0) {
            // Fallback pattern that won't match anything
            return /(?!.*)/g;
        }

        // Build pattern: \b([A-Z][a-zA-Z]*(?:User|Course|...))\b
        const namesPattern = entityNames.join('|');
        return new RegExp(`\\b([A-Z][a-zA-Z]*(?:${namesPattern}))\\b`, 'g');
    }

    /**
     * Detects all entity references in the given code.
     *
     * @param code Java code to scan
     * @param allAggregates All aggregates in the system
     * @returns Array of detected entity names
     */
    static detectEntitiesInCode(code: string, allAggregates: Aggregate[]): string[] {
        const pattern = this.buildEntityPattern(allAggregates);
        const matches = code.matchAll(pattern);
        return Array.from(matches, m => m[1]);
    }

    /**
     * Checks if a type name references an entity.
     *
     * @param typeName Type name to check
     * @param allAggregates All aggregates in the system
     * @returns true if the type is an entity
     */
    static isEntityType(typeName: string, allAggregates: Aggregate[]): boolean {
        const allEntityNames = allAggregates
            .flatMap(agg => getEntities(agg).map(e => e.name));
        return allEntityNames.includes(typeName);
    }
}
