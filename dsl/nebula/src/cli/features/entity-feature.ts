import * as path from "node:path";
import { GenerationOptions, Aggregate, GeneratorRegistry } from "../engine/types.js";
import { TemplateGenerators } from "../engine/template-generators.js";
import { FileWriter } from "../utils/file-writer.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../utils/error-handler.js";
import { ServiceExtensionGenerator } from "../generators/microservices/service/extension/service-extension-generator.js";

export class EntityFeature {
    static async generateCoreComponents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        for (const entity of aggregate.entities) {
            const entityOptions = {
                projectName: options.projectName,
                dtoSchemaRegistry: options.dtoSchemaRegistry,
                allEntities: aggregate.entities
            };
            const entityCode = await generators.entityGenerator.generateEntity(entity, entityOptions);
            const entityPath = path.join(aggregatePath, 'aggregate', `${entity.name}.java`);
            await FileWriter.writeGeneratedFile(entityPath, entityCode, `entity ${entity.name}`);

            
            
            const sharedDtoDir = path.join(aggregatePath, '..', '..', 'shared', 'dtos');
            const dtoCode = await generators.dtoGenerator.generateDto(entity, options);
            const dtoPath = path.join(sharedDtoDir, `${entity.name}Dto.java`);
            await FileWriter.writeGeneratedFile(dtoPath, dtoCode, `shared DTO ${entity.name}Dto`);
        }

        const factoryCode = await generators.factoryGenerator.generateFactory(aggregate, {
            ...options
        });
        const factoryPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}Factory.java`);
        await FileWriter.writeGeneratedFile(factoryPath, factoryCode, `factory ${aggregate.name}Factory`);

        const repositoryCode = await generators.repositoryGenerator.generateRepository(aggregate, options);
        const repositoryPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}CustomRepository.java`);
        await FileWriter.writeGeneratedFile(repositoryPath, repositoryCode, `custom repository ${aggregate.name}CustomRepository`);

        const repositoryInterfaceCode = await generators.repositoryInterfaceGenerator.generateRepositoryInterface(aggregate, options);
        const repositoryInterfacePath = path.join(aggregatePath, 'aggregate', `${aggregate.name}Repository.java`);
        await FileWriter.writeGeneratedFile(repositoryInterfacePath, repositoryInterfaceCode, `repository interface ${aggregate.name}Repository`);

        const hasServiceDefinition = (aggregate as any).serviceDefinition;
        if (!hasServiceDefinition) {
            await ErrorHandler.wrapAsync(
                async () => {
                    const serviceCode = await generators.serviceGenerator.generateService(aggregate, options);
                    const servicePath = path.join(aggregatePath, 'service', `${aggregate.name}Service.java`);
                    await FileWriter.writeGeneratedFile(servicePath, serviceCode, `default service ${aggregate.name}Service`);

                    const extensionCode = ServiceExtensionGenerator.generateExtensionCode(aggregate, options.projectName);
                    const extensionPath = path.join(aggregatePath, 'service', ServiceExtensionGenerator.getExtensionFileName(aggregate));
                    await FileWriter.writeGeneratedFileIfAbsent(extensionPath, extensionCode, `service extension ${aggregate.name}ServiceExtension`);
                },
                ErrorUtils.aggregateContext(
                    'generate default service',
                    aggregate.name,
                    'service-generator',
                    { projectName: options.projectName }
                ),
                ErrorSeverity.FATAL
            );
        }

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            ErrorHandler.handle(
                new Error(`No root entity found in aggregate ${aggregate.name}`),
                ErrorUtils.aggregateContext(
                    'find root entity',
                    aggregate.name,
                    'entity-feature',
                    { totalEntities: aggregate.entities.length }
                ),
                ErrorSeverity.FATAL
            );
            return;
        }

        if (aggregate.name !== rootEntity.name) {
            const aggregateBaseClassCode = TemplateGenerators.generateAggregateBaseClass(
                aggregate.name,
                rootEntity.name,
                options.projectName,
                aggregate
            );
            const aggregateBaseClassPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}.java`);
            await FileWriter.writeGeneratedFile(aggregateBaseClassPath, aggregateBaseClassCode, `aggregate base class ${aggregate.name}`);
        }
    }
}
