import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { CollectionMetadataExtractor } from "../collection/collection-metadata-extractor.js";
import { AddMethodGenerator } from "../collection/add-method-generator.js";
import { AddBatchMethodGenerator } from "../collection/add-batch-method-generator.js";
import { GetMethodGenerator } from "../collection/get-method-generator.js";
import { RemoveMethodGenerator } from "../collection/remove-method-generator.js";
import { UpdateMethodGenerator } from "../collection/update-method-generator.js";

/**
 * Service Collection Generator (Orchestrator)
 *
 * Orchestrates generation of collection manipulation methods by delegating
 * to specialized generators. This class serves as a facade after refactoring.
 *
 * Responsibilities:
 * - Coordinate collection property discovery
 * - Delegate to specialized method generators:
 *   - AddMethodGenerator: Single element add
 *   - AddBatchMethodGenerator: Batch add
 *   - GetMethodGenerator: Element retrieval
 *   - RemoveMethodGenerator: Element removal
 *   - UpdateMethodGenerator: Element update
 */
export class ServiceCollectionGenerator {
    /**
     * Generate all collection manipulation methods for an aggregate.
     *
     * For each collection property, generates 5 methods:
     * 1. add{Element} - Add single element
     * 2. add{Element}s - Add multiple elements
     * 3. get{Element} - Get element by identifier
     * 4. remove{Element} - Remove element
     * 5. update{Element} - Update element
     *
     * Delegates to specialized generators for each operation type.
     *
     * @param aggregateName Aggregate name
     * @param rootEntity Root entity
     * @param projectName Project name for exception handling
     * @param aggregate Full aggregate for entity lookup
     * @returns Generated Java methods as a string
     */
    static generateCollectionMethods(aggregateName: string, rootEntity: Entity, projectName: string, aggregate?: Aggregate): string {
        if (!rootEntity.properties || !aggregate) {
            return '';
        }

        const methods: string[] = [];
        const collections = CollectionMetadataExtractor.findCollectionProperties(rootEntity, aggregate);

        // Create generator instances (extends GeneratorBase)
        const addGenerator = new AddMethodGenerator();
        const addBatchGenerator = new AddBatchMethodGenerator();
        const getGenerator = new GetMethodGenerator();
        const removeGenerator = new RemoveMethodGenerator();
        const updateGenerator = new UpdateMethodGenerator();

        for (const collection of collections) {
            // Generate 5 methods per collection using specialized generators
            methods.push(addGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(addBatchGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(getGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(removeGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(updateGenerator.generate(collection, aggregateName, rootEntity, projectName, aggregate));
        }

        return methods.join('\n\n');
    }
}
