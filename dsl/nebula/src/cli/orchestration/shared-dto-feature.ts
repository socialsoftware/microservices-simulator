import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../core/types.js";
import { SharedDtoGenerator } from "../generator/impl/shared/shared-dto-generator.js";
import { SharedDtos, Model } from "../../language/generated/ast.js";

export class SharedDtoFeature {
    /**
     * Generates shared DTOs from DSL definitions
     */
    static async generateSharedDtos(
        models: Model[],
        paths: any,
        options: GenerationOptions
    ): Promise<void> {
        const sharedDtoPath = path.join(paths.javaPath, 'shared', 'dtos');

        // Create shared/dtos directory
        await fs.mkdir(sharedDtoPath, { recursive: true });

        // Find SharedDtos definitions in all models
        const allSharedDtos: SharedDtos[] = [];
        for (const model of models) {
            if (model.sharedDtos) {
                allSharedDtos.push(...model.sharedDtos);
            }
        }

        if (allSharedDtos.length === 0) {
            console.log('\t- No SharedDtos definitions found, using legacy generation');
            await this.generateLegacySharedDtos(models, paths, options);
            return;
        }

        // Generate DTOs from DSL definitions
        const generator = new SharedDtoGenerator();

        // Collect all DTO definitions for cross-referencing
        const allDtoDefinitions = allSharedDtos.flatMap(block => block.dtos);

        for (const sharedDtosBlock of allSharedDtos) {
            for (const dtoDefinition of sharedDtosBlock.dtos) {
                try {
                    const dtoCode = await generator.generateSharedDtoFromDefinition(
                        dtoDefinition,
                        options,
                        allDtoDefinitions,
                        models  // Pass the models so generator can read aggregate structure!
                    );

                    const dtoFilePath = path.join(sharedDtoPath, `${dtoDefinition.name}.java`);
                    await fs.writeFile(dtoFilePath, dtoCode, 'utf-8');
                    console.log(`\t- Generated shared DTO ${dtoDefinition.name}`);
                } catch (error) {
                    console.error(`\t- Error generating shared DTO ${dtoDefinition.name}: ${error instanceof Error ? error.message : String(error)}`);
                }
            }
        }
    }


    /**
     * Legacy method for backward compatibility when no SharedDtos blocks are defined
     */
    private static async generateLegacySharedDtos(
        models: Model[],
        paths: any,
        options: GenerationOptions
    ): Promise<void> {
        const generator = new SharedDtoGenerator();
        const sharedDtoPath = path.join(paths.javaPath, 'shared', 'dtos');

        // Collect all entities that need shared DTOs
        const sharedDtos = new Map<string, any>();

        for (const model of models) {
            for (const aggregate of model.aggregates) {
                const rootEntity = aggregate.entities?.find((e: any) => e.isRoot);
                if (rootEntity) {
                    const dtoName = `${rootEntity.name}Dto`;
                    if (SharedDtoGenerator.isSharedDto(dtoName)) {
                        sharedDtos.set(dtoName, {
                            name: dtoName,
                            fields: rootEntity.properties || []
                        });
                    }
                }
            }
        }

        // Special case for UserDto - always include it
        if (!sharedDtos.has('UserDto')) {
            sharedDtos.set('UserDto', {
                name: 'UserDto',
                fields: [
                    { type: 'Integer', name: 'id' },
                    { type: 'String', name: 'name' },
                    { type: 'String', name: 'username' },
                    { type: 'String', name: 'email' }
                ]
            });
        }

        // Generate each shared DTO
        for (const [dtoName, dtoInfo] of sharedDtos) {
            try {
                const dtoCode = await generator.generateSharedDto(
                    dtoInfo.name,
                    dtoInfo.fields,
                    options
                );

                const dtoFilePath = path.join(sharedDtoPath, `${dtoName}.java`);
                await fs.writeFile(dtoFilePath, dtoCode, 'utf-8');
                console.log(`\t- Generated shared DTO ${dtoName}`);
            } catch (error) {
                console.error(`\t- Error generating shared DTO ${dtoName}: ${error instanceof Error ? error.message : String(error)}`);
            }
        }
    }
}
