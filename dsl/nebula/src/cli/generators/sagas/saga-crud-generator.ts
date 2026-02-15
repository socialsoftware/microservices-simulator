import type { Aggregate } from '../../../language/generated/ast.js';
import { SagaGenerationOptions } from './saga-generator.js';
import { SagaCreateGenerator } from './operations/saga-create-generator.js';
import { SagaReadGenerator } from './operations/saga-read-generator.js';
import { SagaReadAllGenerator } from './operations/saga-read-all-generator.js';
import { SagaUpdateGenerator } from './operations/saga-update-generator.js';
import { SagaDeleteGenerator } from './operations/saga-delete-generator.js';

/**
 * Orchestrates generation of saga functionality classes for CRUD operations.
 * Refactored to use specialized generators following Template Method pattern.
 */
export class SagaCrudGenerator {
    private createGenerator: SagaCreateGenerator;
    private readGenerator: SagaReadGenerator;
    private readAllGenerator: SagaReadAllGenerator;
    private updateGenerator: SagaUpdateGenerator;
    private deleteGenerator: SagaDeleteGenerator;

    constructor() {
        this.createGenerator = new SagaCreateGenerator();
        this.readGenerator = new SagaReadGenerator();
        this.readAllGenerator = new SagaReadAllGenerator();
        this.updateGenerator = new SagaUpdateGenerator();
        this.deleteGenerator = new SagaDeleteGenerator();
    }

    /**
     * Generate all CRUD saga functionality classes for an aggregate.
     *
     * @param aggregate - The aggregate to generate saga functionalities for
     * @param options - Generation options
     * @param packageName - Target package name
     * @param allAggregates - All aggregates (for cross-aggregate references)
     * @returns Map of filename to file content
     */
    generateCrudSagaFunctionalities(
        aggregate: any,
        options: SagaGenerationOptions,
        packageName: string,
        allAggregates?: Aggregate[]
    ): Record<string, string> {
        const outputs: Record<string, string> = {};

        // Generate Create functionality
        const createResult = this.createGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[createResult.fileName] = createResult.content;

        // Generate Read (getById) functionality
        const readResult = this.readGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[readResult.fileName] = readResult.content;

        // Generate ReadAll (getAll) functionality
        const readAllResult = this.readAllGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[readAllResult.fileName] = readAllResult.content;

        // Generate Update functionality
        const updateResult = this.updateGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[updateResult.fileName] = updateResult.content;

        // Generate Delete functionality
        const deleteResult = this.deleteGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[deleteResult.fileName] = deleteResult.content;

        return outputs;
    }
}
