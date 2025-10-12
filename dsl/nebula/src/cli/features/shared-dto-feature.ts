import { GenerationOptions } from "../engine/types.js";
import { SharedDtoGenerator } from "../generators/microservices/shared/shared-dto-generator.js";
import { SharedDtos, Model } from "../../language/generated/ast.js";
import { FeatureBase, FeatureResult } from "./feature-base.js";

export class SharedDtoFeature extends FeatureBase {
    constructor() {
        super('shared-dto');
    }
    /**
     * Generate shared DTOs for models
     */
    async generate(
        models: Model[],
        paths: any,
        options: GenerationOptions
    ): Promise<FeatureResult> {
        return this.executeFeatureGeneration('shared-dtos', async () => {
            const sharedDtoPath = this.buildPackagePath(paths.javaPath, 'shared', 'dtos');

            // Create shared/dtos directory
            await this.createDirectory(sharedDtoPath, 'shared DTOs directory');

            // Find SharedDtos definitions in all models
            const allSharedDtos: SharedDtos[] = [];
            for (const model of models) {
                if (model.sharedDtos) {
                    allSharedDtos.push(...model.sharedDtos);
                }
            }

            if (allSharedDtos.length === 0) {
                this.logWarning('No SharedDtos definitions found, using legacy generation');
                return await this.generateLegacySharedDtos(models, paths, options);
            }

            // Generate DTOs from DSL definitions
            const generator = new SharedDtoGenerator();
            let filesGenerated = 0;
            const warnings: string[] = [];

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

                        const dtoFilePath = this.buildJavaFilePath(
                            paths.javaPath,
                            ['shared', 'dtos'],
                            dtoDefinition.name
                        );

                        await this.writeGeneratedFile(
                            dtoFilePath,
                            dtoCode,
                            `shared DTO ${dtoDefinition.name}`
                        );
                        filesGenerated++;
                    } catch (error) {
                        const errorMsg = `Error generating shared DTO ${dtoDefinition.name}: ${error instanceof Error ? error.message : String(error)}`;
                        warnings.push(errorMsg);
                        this.logWarning(errorMsg);
                    }
                }
            }

            return this.createSuccessResult(filesGenerated, warnings);
        });
    }


    /**
     * Legacy method for backward compatibility when no SharedDtos blocks are defined
     */
    private async generateLegacySharedDtos(
        models: Model[],
        paths: any,
        options: GenerationOptions
    ): Promise<FeatureResult> {
        const generator = new SharedDtoGenerator();

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
        let filesGenerated = 0;
        const warnings: string[] = [];

        for (const [dtoName, dtoInfo] of sharedDtos) {
            try {
                const dtoCode = await generator.generateSharedDto(
                    dtoInfo.name,
                    dtoInfo.fields,
                    options
                );

                const dtoFilePath = this.buildJavaFilePath(
                    paths.javaPath,
                    ['shared', 'dtos'],
                    dtoName.replace('.java', '')
                );

                await this.writeGeneratedFile(
                    dtoFilePath,
                    dtoCode,
                    `shared DTO ${dtoName}`
                );
                filesGenerated++;
            } catch (error) {
                const errorMsg = `Error generating shared DTO ${dtoName}: ${error instanceof Error ? error.message : String(error)}`;
                warnings.push(errorMsg);
                this.logWarning(errorMsg);
            }
        }

        return this.createSuccessResult(filesGenerated, warnings);
    }

    /**
     * Static method for backward compatibility
     */
    static async generateSharedDtos(
        models: Model[],
        paths: any,
        options: GenerationOptions
    ): Promise<void> {
        const feature = new SharedDtoFeature();
        const result = await feature.generate(models, paths, options);

        if (!result.success) {
            throw new Error(`Shared DTO generation failed: ${result.errors.join(', ')}`);
        }
    }
}
