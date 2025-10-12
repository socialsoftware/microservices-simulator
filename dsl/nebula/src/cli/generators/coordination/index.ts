import { Aggregate } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { FunctionalitiesGenerator } from './functionalities-generator.js';
import { EventProcessingGenerator } from './event-processing-generator.js';

export class CoordinationGenerator {
    private functionalitiesGenerator: FunctionalitiesGenerator;
    private eventProcessingGenerator: EventProcessingGenerator;

    constructor() {
        this.functionalitiesGenerator = new FunctionalitiesGenerator();
        this.eventProcessingGenerator = new EventProcessingGenerator();
    }

    async generateCoordination(aggregate: Aggregate, options: CoordinationGenerationOptions, allAggregates?: Aggregate[]): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string } = {};

        results['functionalities'] = await this.functionalitiesGenerator.generate(aggregate, rootEntity, options, allAggregates);
        results['event-processing'] = await this.eventProcessingGenerator.generate(aggregate, rootEntity, options);

        if (options.architecture === 'causal-saga' || options.features?.includes('sagas')) {
            // Saga coordination is handled by the SagaFeature
            console.log(`\t- Saga coordination handled by SagaFeature for ${aggregate.name}`);
        }

        return results;
    }

}
