import { GenerationOptions, Aggregate, ProjectPaths, GeneratorRegistry } from "./types.js";
import {
    EntityFeature,
    EventsFeature,
    CoordinationFeature,
    WebApiFeature,
    ValidationFeature,
    SagaFeature,
    ServiceFeature
} from "../features/index.js";
import { SharedFeature } from "../generators/microservices/shared/index.js";

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

    static async generateSharedComponents(paths: ProjectPaths, options: GenerationOptions, models?: any[]): Promise<void> {
        // Always generate shared components for now - they are essential
        // TODO: Make this configurable once the feature system is properly integrated
        if (true) {
            const sharedFeature = new SharedFeature();
            const sharedResults = await sharedFeature.generateSharedComponents({
                projectName: options.projectName,
                outputPath: options.outputPath,
                features: options.features,
                models: models
            });

            // Write shared components to files
            for (const [filePath, content] of Object.entries(sharedResults)) {
                const fullPath = `${paths.javaPath}/${filePath}`;
                const fs = await import('node:fs/promises');
                const path = await import('node:path');

                // Ensure directory exists
                await fs.mkdir(path.dirname(fullPath), { recursive: true });
                await fs.writeFile(fullPath, content, 'utf-8');
                console.log(`\t- Generated shared component ${filePath}`);
            }
        }
    }
}
