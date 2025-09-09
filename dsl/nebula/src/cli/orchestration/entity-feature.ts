import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions, Aggregate, GeneratorRegistry } from "../core/types.js";
import { TemplateGenerators } from "../core/template-generators.js";

export class EntityFeature {
    static async generateCoreComponents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        for (const entity of aggregate.entities) {
            const entityCode = await generators.entityGenerator.generateEntity(entity, options);
            const entityPath = path.join(aggregatePath, 'aggregate', `${entity.name}.java`);
            await fs.mkdir(path.dirname(entityPath), { recursive: true });
            await fs.writeFile(entityPath, entityCode, 'utf-8');
            console.log(`\t- Generated entity ${entity.name}`);

            const dtoCode = await generators.dtoGenerator.generateDto(entity, options);
            const dtoPath = path.join(aggregatePath, 'aggregate', `${entity.name}Dto.java`);
            await fs.writeFile(dtoPath, dtoCode, 'utf-8');
            console.log(`\t- Generated DTO ${entity.name}Dto`);
        }

        const factoryCode = await generators.factoryGenerator.generateFactory(aggregate, options);
        const factoryPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}Factory.java`);
        await fs.writeFile(factoryPath, factoryCode, 'utf-8');
        console.log(`\t- Generated factory ${aggregate.name}Factory`);

        const repositoryCode = await generators.repositoryGenerator.generateRepository(aggregate, options);
        const repositoryPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}CustomRepository.java`);
        await fs.writeFile(repositoryPath, repositoryCode, 'utf-8');
        console.log(`\t- Generated custom repository ${aggregate.name}CustomRepository`);

        const repositoryInterfaceCode = await generators.repositoryInterfaceGenerator.generateRepositoryInterface(aggregate, options);
        const repositoryInterfacePath = path.join(aggregatePath, 'aggregate', `${aggregate.name}Repository.java`);
        await fs.writeFile(repositoryInterfacePath, repositoryInterfaceCode, 'utf-8');
        console.log(`\t- Generated repository interface ${aggregate.name}Repository`);

        const hasServiceDefinition = (aggregate as any).serviceDefinition;
        if (!hasServiceDefinition) {
            console.log(`\t- Generating default service with options:`, {
                projectName: options.projectName,
                architecture: options.architecture,
                features: options.features?.slice(0, 3)
            });
            try {
                const serviceCode = await generators.serviceGenerator.generateService(aggregate, options);
                const servicePath = path.join(aggregatePath, 'service', `${aggregate.name}Service.java`);
                await fs.mkdir(path.dirname(servicePath), { recursive: true });
                await fs.writeFile(servicePath, serviceCode, 'utf-8');
                console.log(`\t- Generated default service ${aggregate.name}Service`);
            } catch (error) {
                console.error(`Error generating service for ${aggregate.name}:`, error);
                if (error instanceof Error) {
                    console.error('Stack trace:', error.stack);
                }
                throw error;
            }
        } else {
            console.log(`\t- Service definition found in DSL for ${aggregate.name}, skipping default service generation`);
        }

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        if (aggregate.name !== rootEntity.name) {
            const aggregateBaseClassCode = TemplateGenerators.generateAggregateBaseClass(
                aggregate.name,
                rootEntity.name,
                options.projectName
            );
            const aggregateBaseClassPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}.java`);
            await fs.writeFile(aggregateBaseClassPath, aggregateBaseClassCode, 'utf-8');
            console.log(`\t- Generated aggregate base class ${aggregate.name}`);
        }
    }
}
