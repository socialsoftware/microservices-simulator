import { GenerationOptions, Aggregate, GeneratorRegistry } from "../engine/types.js";
import { TemplateGenerators } from "../engine/template-generators.js";
import { FileWriter } from "../utils/file-writer.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../utils/error-handler.js";
import { ServiceExtensionGenerator } from "../generators/microservices/service/extension/service-extension-generator.js";
import { AggregatePaths } from "../utils/path-builder.js";

export class EntityFeature {
    static async generateCoreComponents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        const paths = new AggregatePaths(aggregatePath, aggregate.name);

        for (const entity of aggregate.entities) {
            const entityOptions = {
                projectName: options.projectName,
                dtoSchemaRegistry: options.dtoSchemaRegistry,
                allEntities: aggregate.entities
            };
            const entityCode = await generators.entityGenerator.generateEntity(entity, entityOptions);
            await FileWriter.writeGeneratedFile(paths.entity(entity.name), entityCode, `entity ${entity.name}`);

            const dtoCode = await generators.dtoGenerator.generateDto(entity, options);
            await FileWriter.writeGeneratedFile(paths.sharedDto(entity.name), dtoCode, `shared DTO ${entity.name}Dto`);
        }

        const factoryCode = await generators.factoryGenerator.generateFactory(aggregate, { ...options });
        await FileWriter.writeGeneratedFile(paths.factory(), factoryCode, `factory ${aggregate.name}Factory`);

        const repositoryCode = await generators.repositoryGenerator.generateRepository(aggregate, options);
        await FileWriter.writeGeneratedFile(paths.customRepository(), repositoryCode, `custom repository ${aggregate.name}CustomRepository`);

        const repositoryInterfaceCode = await generators.repositoryInterfaceGenerator.generateRepositoryInterface(aggregate, options);
        await FileWriter.writeGeneratedFile(paths.repositoryInterface(), repositoryInterfaceCode, `repository interface ${aggregate.name}Repository`);

        const hasServiceDefinition = (aggregate as any).serviceDefinition;
        if (!hasServiceDefinition) {
            await ErrorHandler.wrapAsync(
                async () => {
                    const serviceCode = await generators.serviceGenerator.generateService(aggregate, options);
                    await FileWriter.writeGeneratedFile(paths.service(), serviceCode, `default service ${aggregate.name}Service`);

                    const extensionCode = ServiceExtensionGenerator.generateExtensionCode(aggregate, options.projectName);
                    const extensionPath = paths.serviceExtension(ServiceExtensionGenerator.getExtensionFileName(aggregate));
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
            await FileWriter.writeGeneratedFile(paths.aggregateBaseClass(), aggregateBaseClassCode, `aggregate base class ${aggregate.name}`);
        }
    }
}
