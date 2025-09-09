import { GenerationOptions, Aggregate, ProjectPaths, GeneratorRegistry } from "./types.js";
import {
    EntityFeature,
    EventsFeature,
    CoordinationFeature,
    WebApiFeature,
    ValidationFeature,
    SagaFeature,
    ServiceFeature
} from "../orchestration/index.js";

export class FeatureGenerators {
    static async generateAggregate(aggregate: Aggregate, paths: ProjectPaths, options: GenerationOptions, generators: GeneratorRegistry, allAggregates?: Aggregate[]): Promise<void> {
        const aggregatePath = paths.javaPath + '/microservices/' + aggregate.name.toLowerCase();

        await EntityFeature.generateCoreComponents(aggregate, aggregatePath, options, generators);

        await ServiceFeature.generateService(aggregate, aggregatePath, options, generators);

        if (options.features.includes('events')) {
            await EventsFeature.generateEvents(aggregate, aggregatePath, options, generators);
        }

        if (options.features.includes('coordination')) {
            await CoordinationFeature.generateCoordination(aggregate, paths, options, generators, allAggregates);
        }

        if (options.architecture.includes('causal') || options.architecture.includes('saga')) {
            await SagaFeature.generateCausalEntities(aggregate, paths, options, generators);
        }

        if (options.features.includes('webapi')) {
            await WebApiFeature.generateWebApi(aggregate, paths, options, generators);
        }

        if (options.features.includes('validation')) {
            await ValidationFeature.generateValidation(aggregate, paths, options, generators);
        }

        if (options.features.includes('saga')) {
            await SagaFeature.generateSaga(aggregate, paths, options, generators);
        }
    }

    static async generateGlobalWebApi(paths: ProjectPaths, options: GenerationOptions, generators: GeneratorRegistry): Promise<void> {
        await WebApiFeature.generateGlobalWebApi(paths, options, generators);
    }
}
