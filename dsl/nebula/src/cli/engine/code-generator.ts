import type { Model } from "../../language/generated/ast.js";
import { createNebulaServices } from "../../language/nebula-module.js";
import { extractAstNode } from "../utils/cli-util.js";
import { collectNebulaFiles } from "../utils/file-utils.js";
import { NodeFileSystem } from "langium/node";
import { URI } from "langium";
import * as fs from "node:fs/promises";
import * as path from "node:path";

import { TemplateGenerateOptions, GenerationOptions, DEFAULT_OUTPUT_DIR } from "./types.js";
import { ProjectSetup } from "./project-setup.js";
import { GeneratorRegistryFactory } from "./generator-registry.js";
import { TemplateGenerators } from "./template-generators.js";
import { FeatureGenerators } from "./feature-generators.js";
import { ConfigLoader } from "../utils/config-loader.js";
import { getGlobalConfig } from "../generators/shared/config.js";

export class CodeGenerator {
    static async generateCode(inputPath: string, opts: TemplateGenerateOptions): Promise<void> {
        try {
            console.log(`Starting generation for: ${inputPath}`);

            const nebulaFiles = await collectNebulaFiles(inputPath);
            console.log(`Found ${nebulaFiles.length} Nebula files:`, nebulaFiles);

            if (nebulaFiles.length === 0) {
                console.error(`No Nebula files found at path: ${inputPath}`);
                process.exit(1);
            }

            const services = createNebulaServices(NodeFileSystem).nebulaServices;
            await this.loadLanguageDocuments(services, nebulaFiles);

            // Load configuration from file and merge with CLI options
            const cliConfig = this.extractConfiguration(opts, inputPath);
            const loadedConfig = await ConfigLoader.loadConfig(inputPath, {
                projectName: cliConfig.projectName,
                outputDirectory: cliConfig.baseOutputDir,
                architecture: cliConfig.architecture as any,
                features: cliConfig.features as any
            });

            const config = {
                ...cliConfig,
                basePackage: loadedConfig.basePackage,
                consistencyModels: loadedConfig.consistencyModels
            };

            const paths = await ProjectSetup.setupProjectPaths(
                config.baseOutputDir,
                inputPath,
                config.projectName
            );

            const allModels = await this.parseModels(nebulaFiles, services);

            if (config.validate) {
                await this.validateModels(allModels, config);
            }

            const generators = GeneratorRegistryFactory.createRegistry();
            await this.generateAllCode(allModels, paths, config, generators);

            console.log(`\nCode generation completed successfully!`);
            console.log(`Output directory: ${paths.projectPath}`);
            console.log(`Base package: ${config.basePackage || getGlobalConfig().getBasePackage()}`);
            console.log(`Architecture: ${config.architecture}`);
            console.log(`Features: ${config.features.join(', ')}`);

        } catch (error) {
            console.error(`Error processing path ${inputPath}: ${error}`);
            process.exit(1);
        }
    }

    private static async loadLanguageDocuments(services: any, nebulaFiles: string[]): Promise<void> {
        const absFiles = nebulaFiles.map(f => path.resolve(f));

        for (const filePath of absFiles) {
            await services.shared.workspace.LangiumDocuments.getOrCreateDocument(
                URI.file(filePath)
            );
        }

        await services.shared.workspace.DocumentBuilder.build(
            Array.from(services.shared.workspace.LangiumDocuments.all),
            { validation: true }
        );
    }

    private static extractConfiguration(opts: TemplateGenerateOptions, inputPath: string) {
        const baseOutputDir = opts.destination || DEFAULT_OUTPUT_DIR;
        const projectName = ProjectSetup.deriveProjectName(inputPath, opts.name);
        const architecture = opts.architecture || 'default';
        const features = opts.features || ['events', 'validation', 'webapi', 'coordination', 'saga', 'shared'];
        const validate = opts.validate || false;

        return {
            baseOutputDir,
            projectName,
            architecture,
            features,
            validate
        };
    }

    private static async parseModels(nebulaFiles: string[], services: any): Promise<Model[]> {
        const allModels: Model[] = [];

        for (const filePath of nebulaFiles) {
            const model = await extractAstNode<Model>(filePath, services);
            allModels.push(model);
        }

        return allModels;
    }

    private static async validateModels(allModels: Model[], config: any): Promise<void> {
        console.log("Validating DSL files...");
        const generators = GeneratorRegistryFactory.createRegistry();

        for (const model of allModels) {
            for (const aggregate of model.aggregates) {
                const validationResult = await generators.validationSystem.validateAggregate(aggregate, {
                    projectName: config.projectName,
                    architecture: config.architecture,
                    features: config.features
                });

                if (!validationResult.isValid) {
                    console.error(`Validation failed for aggregate ${aggregate.name}:`);
                    console.error(generators.validationSystem.getValidationReport(validationResult));
                    process.exit(1);
                }
            }
        }

        console.log("Validation passed!");
    }

    private static async generateAllCode(
        allModels: Model[],
        paths: any,
        config: any,
        generators: any
    ): Promise<void> {
        // STEP 1: Extract all shared DTOs and mappings from DSL
        const sharedMetadata = this.extractSharedMetadata(allModels);

        const options: GenerationOptions = {
            architecture: config.architecture as 'default' | 'external-dto-removal' | 'causal-saga',
            features: config.features || [],
            projectName: config.projectName,
            outputPath: paths.projectPath,
            consistencyModels: config.consistencyModels,
            // Add shared metadata to options so all generators can access it
            allSharedDtos: sharedMetadata.allDtoDefinitions,
            dtoMappings: sharedMetadata.allDtoMappings,
            allModels: allModels
        };

        const allAggregates = allModels.flatMap(model => model.aggregates);

        // STEP 2: Generate aggregates with access to shared metadata
        for (const model of allModels) {
            for (const aggregate of model.aggregates) {
                console.log(`\nGenerating ${aggregate.name} aggregate:`);
                await FeatureGenerators.generateAggregate(aggregate, paths, options, generators, allAggregates);
            }
        }

        // STEP 3: Generate project files
        await this.generateProjectFiles(allModels, paths, config, generators);

        // STEP 4: Generate shared components
        await FeatureGenerators.generateSharedComponents(paths, options, allModels);
    }

    /**
     * Extract shared DTOs and mappings from all models before generation
     */
    private static extractSharedMetadata(allModels: Model[]): {
        allDtoDefinitions: any[];
        allDtoMappings: any[];
    } {
        const allDtoDefinitions: any[] = [];
        const allDtoMappings: any[] = [];

        for (const model of allModels) {
            if (model.sharedDtos) {
                for (const sharedDtosBlock of model.sharedDtos) {
                    if (sharedDtosBlock.dtos) {
                        for (const dtoDefinition of sharedDtosBlock.dtos) {
                            allDtoDefinitions.push(dtoDefinition);

                            // Extract mappings from this DTO
                            if (dtoDefinition.mappings) {
                                allDtoMappings.push(...dtoDefinition.mappings);
                            }
                        }
                    }
                }
            }
        }

        return { allDtoDefinitions, allDtoMappings };
    }

    private static async generateProjectFiles(
        allModels: Model[],
        paths: any,
        config: any,
        generators: any
    ): Promise<void> {
        const options = {
            architecture: config.architecture,
            features: config.features,
            projectName: config.projectName,
            outputPath: paths.projectPath
        };

        if (allModels[0]?.aggregates?.length > 0) {
            const integrationCode = await generators.integrationGenerator.generateIntegration(
                allModels[0].aggregates[0],
                options
            );
            const capitalizedProjectName = config.projectName.charAt(0).toUpperCase() + config.projectName.slice(1);
            const integrationPath = path.join(paths.javaPath, `${capitalizedProjectName}Simulator.java`);
            await fs.writeFile(integrationPath, integrationCode['application'], 'utf-8');
            console.log(`\t- Generated integration ${config.projectName}Simulator`);

            const globalConfig = getGlobalConfig();
            await generators.exceptionGenerator.generate(
                allModels[0].aggregates[0],
                paths.projectPath,
                {
                    projectName: config.projectName,
                    packageName: globalConfig.buildPackageName(config.projectName),
                    architecture: config.architecture,
                    features: config.features || []
                },
                allModels
            );
        }

        if (config.features?.includes('webapi')) {
            // Generate shared DTOs first
            const { SharedDtoFeature } = await import('../features/shared-dto-feature.js');
            await SharedDtoFeature.generateSharedDtos(allModels, paths, options);

            await FeatureGenerators.generateGlobalWebApi(paths, options, generators);
        }

        const pomContent = TemplateGenerators.generatePomXml(config.projectName, config.architecture, config.features);
        await fs.writeFile(path.join(paths.projectPath, "pom.xml"), pomContent, 'utf-8');
        console.log(`\t- Generated pom.xml`);

        const gitignoreContent = TemplateGenerators.generateGitignore();
        await fs.writeFile(path.join(paths.projectPath, ".gitignore"), gitignoreContent, 'utf-8');
        console.log(`\t- Generated .gitignore`);

        await this.generateConfigurationFiles(paths, config, generators);

        await this.generateTestTemplates(allModels, paths, config, generators);
    }

    private static async generateConfigurationFiles(paths: any, config: any, generators: any): Promise<void> {
        try {
            await generators.configurationGenerator.generate({
                projectName: config.projectName,
                architecture: config.architecture,
                features: config.features || [],
                outputDirectory: paths.projectPath
            }, {
                projectName: config.projectName,
                architecture: config.architecture,
                features: config.features || []
            });
            console.log(`\t- Generated configuration files`);
        } catch (error) {
            console.error(`\t- Error generating configuration files: ${error instanceof Error ? error.message : String(error)}`);
        }
    }

    private static async generateTestTemplates(allModels: Model[], paths: any, config: any, generators: any): Promise<void> {
        try {
            const mainAggregate = allModels[0]?.aggregates[0];
            if (mainAggregate) {
                await generators.testGenerator.generate({
                    aggregate: mainAggregate,
                    projectName: config.projectName,
                    outputDirectory: paths.projectPath,
                    features: config.features || []
                }, {
                    projectName: config.projectName,
                    architecture: config.architecture,
                    features: config.features || []
                });
                console.log(`\t- Generated test templates`);
            }
        } catch (error) {
            console.error(`\t- Error generating test templates: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
