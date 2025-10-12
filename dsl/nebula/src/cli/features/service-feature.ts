import * as fs from 'fs/promises';
import * as path from 'path';
import { GenerationOptions, Aggregate, GeneratorRegistry } from '../engine/types.js';

export class ServiceFeature {
    static async generateService(aggregate: Aggregate, aggregatePath: string, options: GenerationOptions, generators: GeneratorRegistry): Promise<void> {
        try {
            const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
            if (!rootEntity) {
                throw new Error(`No root entity found in aggregate ${aggregate.name}`);
            }

            const serviceDefinition = (aggregate as any).serviceDefinition;
            if (!serviceDefinition) {
                console.log(`\t- No service definition found for ${aggregate.name}, skipping service generation`);
                return;
            }

            const serviceCode = await generators.serviceDefinitionGenerator.generateServiceFromDefinition(
                aggregate,
                rootEntity,
                options
            );

            const servicePath = path.join(aggregatePath, 'service', `${serviceDefinition.name || aggregate.name + 'Service'}.java`);
            await fs.mkdir(path.dirname(servicePath), { recursive: true });
            await fs.writeFile(servicePath, serviceCode, 'utf-8');
            console.log(`\t- Generated service ${serviceDefinition.name || aggregate.name + 'Service'}`);

        } catch (error) {
            console.error(`\t- Error generating service for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
