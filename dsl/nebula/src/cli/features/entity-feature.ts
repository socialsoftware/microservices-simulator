import * as path from "node:path";
import { GenerationOptions, Aggregate, GeneratorRegistry } from "../engine/types.js";
import { TemplateGenerators } from "../engine/template-generators.js";
import { FileWriter } from "../utils/file-writer.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../utils/error-handler.js";

export class EntityFeature {
    /**
     * Check if a DTO is available as a shared DTO
     */
    private static isSharedDto(dtoName: string): boolean {
        const sharedDtos = [
            'UserDto',
            'CourseDto',
            'ExecutionDto',
            'QuestionDto',
            'TopicDto',
            'QuizDto',
            'TournamentDto',
            'AnswerDto'
        ];
        return sharedDtos.includes(dtoName);
    }

    static async generateCoreComponents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        for (const entity of aggregate.entities) {
            // Pass shared metadata to entity generator
            const entityOptions = {
                projectName: options.projectName,
                allSharedDtos: options.allSharedDtos,
                dtoMappings: options.dtoMappings,
                allEntities: aggregate.entities
            };
            const entityCode = await generators.entityGenerator.generateEntity(entity, entityOptions);
            const entityPath = path.join(aggregatePath, 'aggregate', `${entity.name}.java`);
            await FileWriter.writeGeneratedFile(entityPath, entityCode, `entity ${entity.name}`);

            // Only generate DTO for root entities, and only if not available as shared DTO
            if ((entity as any).isRoot) {
                if (!this.isSharedDto(entity.name + 'Dto')) {
                    const dtoCode = await generators.dtoGenerator.generateDto(entity, options);
                    const dtoPath = path.join(aggregatePath, 'aggregate', `${entity.name}Dto.java`);
                    await FileWriter.writeGeneratedFile(dtoPath, dtoCode, `DTO ${entity.name}Dto`);
                } else {
                    console.log(`\t- Skipping DTO generation for ${entity.name}Dto (using shared DTO)`);
                }
            } else {
                console.log(`\t- Skipping DTO generation for ${entity.name} (non-root entity)`);
            }
        }

        const factoryCode = await generators.factoryGenerator.generateFactory(aggregate, {
            ...options,
            allSharedDtos: options.allSharedDtos
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
            console.log(`\t- Generating default service with options:`, {
                projectName: options.projectName
            });
            await ErrorHandler.wrapAsync(
                async () => {
                    const serviceCode = await generators.serviceGenerator.generateService(aggregate, options);
                    const servicePath = path.join(aggregatePath, 'service', `${aggregate.name}Service.java`);
                    await FileWriter.writeGeneratedFile(servicePath, serviceCode, `default service ${aggregate.name}Service`);
                },
                ErrorUtils.aggregateContext(
                    'generate default service',
                    aggregate.name,
                    'service-generator',
                    { projectName: options.projectName }
                ),
                ErrorSeverity.FATAL
            );
        } else {
            console.log(`\t- Service definition found in DSL for ${aggregate.name}, skipping default service generation`);
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
            return; // This will never be reached due to FATAL error, but satisfies TypeScript
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
