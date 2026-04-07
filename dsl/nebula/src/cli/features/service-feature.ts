import chalk from 'chalk';
import * as fs from 'fs/promises';
import * as path from 'path';
import { GenerationOptions, Aggregate, GeneratorRegistry } from '../engine/types.js';
import { FileWriter } from '../utils/file-writer.js';
import { ServiceExtensionGenerator } from '../generators/microservices/service/extension/service-extension-generator.js';

export class ServiceFeature {
    static async generateService(aggregate: Aggregate, aggregatePath: string, options: GenerationOptions, generators: GeneratorRegistry): Promise<void> {
        try {
            const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
            if (!rootEntity) {
                throw new Error(`No root entity found in aggregate ${aggregate.name}`);
            }

            const serviceDefinition = (aggregate as any).serviceDefinition;
            if (!serviceDefinition) {
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

            const extensionCode = ServiceExtensionGenerator.generateExtensionCode(aggregate, options.projectName);
            const extensionPath = path.join(aggregatePath, 'service', ServiceExtensionGenerator.getExtensionFileName(aggregate));
            await FileWriter.writeGeneratedFileIfAbsent(extensionPath, extensionCode, `service extension ${aggregate.name}ServiceExtension`);

        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating service for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`));
        }
    }
}
