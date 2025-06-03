import path from "node:path";
import * as fs from "node:fs/promises";
import type { Model, Entity } from "../../language/generated/ast.js";
import { generateEntityCode } from "./entity-generator.js";
import { generateServiceCode } from "./service-generator.js";
import { logger, getUniqueFilePath } from "./utils.js";

enum AggregateSubdirectory {
    AGGREGATE = 'aggregate',
    EVENTS = 'events',
    SERVICE = 'service'
}

export async function generate(outputPath: string, model: Model, projectName: string): Promise<void> {
    try {
        for (const aggregate of model.aggregates) {
            await generateAggregate(outputPath, aggregate, projectName);
        }
    } catch (error) {
        logger.error(`Error in code generation: ${error}`);
    }
}

async function generateAggregate(outputPath: string, aggregate: any, projectName: string): Promise<void> {
    const aggregateFolderPath = path.join(outputPath, aggregate.name.toLowerCase());
    try {
        logger.info(`Generating ${aggregate.name} aggregate:`);
        await createAggregateDirectories(aggregateFolderPath);
        await generateEntities(aggregateFolderPath, aggregate.entities, projectName);
        await generateService(aggregateFolderPath, aggregate, projectName);
        logger.info(`\n`);
    } catch (error) {
        logger.error(`Error generating ${aggregate.name} aggregate: ${error}`);
    }
}

async function createAggregateDirectories(aggregateFolderPath: string): Promise<void> {
    const subdirectories = [
        AggregateSubdirectory.AGGREGATE,
        AggregateSubdirectory.EVENTS,
        AggregateSubdirectory.SERVICE
    ];

    for (const subdir of subdirectories) {
        const subdirPath = path.join(aggregateFolderPath, subdir);
        await fs.mkdir(subdirPath, { recursive: true });
    }
}

async function generateEntities(aggregateFolderPath: string, entities: Entity[], projectName: string): Promise<void> {
    const aggregateSubdirPath = path.join(aggregateFolderPath, AggregateSubdirectory.AGGREGATE);

    for (const entity of entities) {
        const entityCode = generateEntityCode(entity, projectName);
        const desiredPath = path.join(aggregateSubdirPath, `${entity.name}.java`);
        const filePath = await getUniqueFilePath(desiredPath);
        await fs.writeFile(filePath, entityCode, 'utf-8');
        logger.info(`\t- Generated entity ${entity.name}`);

        // Generate DTO for root entities
        // if (entity.isRoot) {
        //     const dtoCode = generateDtoCode(entity, projectName);
        //     const dtoFilePath = path.join(aggregateSubdirPath, `${entity.name}Dto.java`);
        //     await fs.writeFile(dtoFilePath, dtoCode, 'utf-8');
        //     logger.info(`\t- Generated DTO for root entity ${entity.name}`);
        // }
    }
}


async function generateService(aggregateFolderPath: string, aggregate: any, projectName: string): Promise<void> {
    const serviceSubdirPath = path.join(aggregateFolderPath, AggregateSubdirectory.SERVICE);
    const serviceCode = generateServiceCode(aggregate, projectName);
    const desiredPath = path.join(serviceSubdirPath, `${aggregate.name}Service.java`);
    const filePath = await getUniqueFilePath(desiredPath);
    await fs.writeFile(filePath, serviceCode, 'utf-8');
    logger.info(`\t- Generated service ${aggregate.name}Service`);
} 