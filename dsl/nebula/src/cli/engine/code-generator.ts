import { Model } from "../../language/generated/ast.js";
import { createNebulaServices } from "../../language/nebula-module.js";
import { extractAstNode } from "../utils/cli-util.js";
import { collectNebulaFiles } from "../utils/file-utils.js";
import { NodeFileSystem } from "langium/node";
import { URI, type LangiumDocument } from "langium";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { initializeAggregateProperties } from "../utils/aggregate-helpers.js";

import { TemplateGenerateOptions, GenerationOptions, DEFAULT_OUTPUT_DIR } from "./types.js";
import { ProjectSetup } from "./project-setup.js";
import { GeneratorRegistryFactory } from "./generator-registry.js";
import { TemplateGenerators } from "./template-generators.js";
import { ConfigLoader } from "../utils/config-loader.js";
import { getGlobalConfig } from "../generators/common/config.js";
import {
    EntityFeature,
    EventsFeature,
    CoordinationFeature,
    WebApiFeature,
    ValidationFeature,
    SagaFeature,
    ServiceFeature
} from "../features/index.js";
import { SharedFeature } from "../generators/microservices/shared/index.js";

export class CodeGenerator {
    /**
     * Main entry point for generating Java microservices code from Nebula DSL files.
     * 
     * This method orchestrates the entire code generation process:
     * 1. Discovers and parses DSL files
     * 2. Validates the domain models
     * 3. Generates complete microservices architecture including:
     *    - JPA entities and repositories
     *    - Service layers and business logic
     *    - REST APIs and DTOs
     *    - Event handling and coordination
     *    - Saga patterns and validation
     *    - Project infrastructure (pom.xml, configuration, etc.)
     * 
     * @param inputPath Path to the abstractions folder containing .nebula files
     * @param opts Generation options (destination, project name, validation)
     */
    static async generateCode(inputPath: string, opts: TemplateGenerateOptions): Promise<void> {
        try {
            console.log(`Starting generation for: ${inputPath}`);

            // ═══════════════════════════════════════════════════════════════
            // PHASE 1: DISCOVERY & PARSING
            // ═══════════════════════════════════════════════════════════════

            // Discover all .nebula files in the input directory
            const nebulaFiles = await collectNebulaFiles(inputPath);
            if (nebulaFiles.length === 0) {
                console.error(`No Nebula files found at path: ${inputPath}`);
                process.exit(1);
            }
            console.log(`Found ${nebulaFiles.length} Nebula files`);

            // Initialize Langium language services and parse DSL models
            const services = createNebulaServices(NodeFileSystem).nebulaServices;
            await this.loadLanguageDocuments(services, nebulaFiles);
            const models = await this.parseModels(nebulaFiles, services);

            // ═══════════════════════════════════════════════════════════════
            // PHASE 2: CONFIGURATION & VALIDATION
            // ═══════════════════════════════════════════════════════════════

            // Setup project configuration and output paths
            const config = await this.setupConfiguration(opts, inputPath);
            const paths = await ProjectSetup.setupProjectPaths(config.baseOutputDir, inputPath, config.projectName);

            // Validate domain models for correctness
            await this.validateModels(models, config);

            // ═══════════════════════════════════════════════════════════════
            // PHASE 3: CODE GENERATION
            // ═══════════════════════════════════════════════════════════════

            console.log("Generating code...");
            const generators = GeneratorRegistryFactory.createRegistry();

            const allDtoDefinitions: any[] = [];
            const allDtoMappings: any[] = [];
            for (const model of models) {
                if (model.sharedDtos) {
                    for (const sharedDtosBlock of model.sharedDtos) {
                        if (sharedDtosBlock.dtos) {
                            for (const dtoDefinition of sharedDtosBlock.dtos) {
                                allDtoDefinitions.push(dtoDefinition);
                            }
                        }
                    }
                }
            }

            const options: GenerationOptions = {
                projectName: config.projectName,
                outputPath: paths.projectPath,
                consistencyModels: config.consistencyModels,
                allSharedDtos: allDtoDefinitions,
                dtoMappings: allDtoMappings,
                allModels: models
            };

            const aggregates = models.flatMap(model => model.aggregates);

            // ───────────────────────────────────────────────────────────────
            // 3A. Generate Aggregate-Specific Components
            // ───────────────────────────────────────────────────────────────

            for (const model of models) {
                for (const aggregate of model.aggregates) {
                    initializeAggregateProperties(aggregate);

                    console.log(`\nGenerating ${aggregate.name} components:`);

                    const aggregatePath = paths.javaPath + '/microservices/' + aggregate.name.toLowerCase();

                    // Core microservice components (entities, services, repositories, factories)
                    await EntityFeature.generateCoreComponents(aggregate, aggregatePath, options, generators);
                    await ServiceFeature.generateService(aggregate, aggregatePath, options, generators);

                    // Event-driven architecture components
                    await EventsFeature.generateEvents(aggregate, aggregatePath, options, generators);

                    // Cross-aggregate coordination and communication
                    await CoordinationFeature.generateCoordination(aggregate, paths, options, generators, aggregates);

                    // REST API endpoints and DTOs
                    await WebApiFeature.generateWebApi(aggregate, paths, options, generators);

                    // Business rule validation
                    await ValidationFeature.generateValidation(aggregate, paths, options, generators);

                    // Distributed transaction patterns
                    await SagaFeature.generateSaga(aggregate, paths, options, generators);
                }
            }

            // ───────────────────────────────────────────────────────────────
            // 3B. Generate Project Infrastructure
            // ───────────────────────────────────────────────────────────────

            const projectOptions: GenerationOptions = {
                projectName: config.projectName,
                outputPath: paths.projectPath
            };

            // Application integration and exception handling
            if (models[0]?.aggregates?.length > 0) {
                // Main application class for microservices simulator
                const integrationCode = await generators.integrationGenerator.generateIntegration(
                    models[0].aggregates[0],
                    projectOptions
                );
                const capitalizedProjectName = config.projectName.charAt(0).toUpperCase() + config.projectName.slice(1);
                const integrationPath = path.join(paths.javaPath, `${capitalizedProjectName}Simulator.java`);
                await fs.writeFile(integrationPath, integrationCode['application'], 'utf-8');
                console.log(`\t- Generated integration ${config.projectName}Simulator`);

                // Global exception handling
                const globalConfig = getGlobalConfig();
                await generators.exceptionGenerator.generate(
                    models[0].aggregates[0],
                    paths.projectPath,
                    {
                        projectName: config.projectName,
                        packageName: globalConfig.buildPackageName(config.projectName)
                    },
                    models
                );
            }

            // Global web API components and shared DTOs
            const { SharedDtoFeature } = await import('../features/shared-dto-feature.js');
            await SharedDtoFeature.generateSharedDtos(models, paths, projectOptions);
            await WebApiFeature.generateGlobalWebApi(paths, projectOptions, generators);

            // Maven build configuration
            const pomContent = TemplateGenerators.generatePomXml(config.projectName);
            await fs.writeFile(path.join(paths.projectPath, "pom.xml"), pomContent, 'utf-8');
            console.log(`\t- Generated pom.xml`);

            // Git ignore file
            const gitignoreContent = TemplateGenerators.generateGitignore();
            await fs.writeFile(path.join(paths.projectPath, ".gitignore"), gitignoreContent, 'utf-8');
            console.log(`\t- Generated .gitignore`);

            // Spring Boot configuration files
            try {
                await generators.configurationGenerator.generate({
                    projectName: config.projectName,
                    outputDirectory: paths.projectPath
                }, {
                    projectName: config.projectName
                });
                console.log(`\t- Generated configuration files`);
            } catch (error) {
                // console.error(`\t- Error generating configuration files: ${error instanceof Error ? error.message : String(error)}`);
            }

            // Note: Test generation is currently disabled
            console.log(`\t- Test generation skipped (disabled)`);

            // ───────────────────────────────────────────────────────────────
            // 3C. Generate Shared Components
            // ───────────────────────────────────────────────────────────────

            // Shared enums and utilities used across all microservices
            const sharedFeature = new SharedFeature();
            const sharedResults = await sharedFeature.generateSharedComponents({
                projectName: options.projectName,
                outputPath: options.outputPath,
                models: models
            });
            for (const [filePath, content] of Object.entries(sharedResults)) {
                const fullPath = `${paths.javaPath}/${filePath}`;
                await fs.mkdir(path.dirname(fullPath), { recursive: true });
                await fs.writeFile(fullPath, content, 'utf-8');
                console.log(`\t- Generated shared component ${filePath}`);
            }

            // ═══════════════════════════════════════════════════════════════
            // PHASE 4: COMPLETION
            // ═══════════════════════════════════════════════════════════════

            console.log(`\nCode generation completed successfully!`);
            console.log(`Output: ${paths.projectPath}`);
            console.log(`Package: ${config.basePackage || getGlobalConfig().getBasePackage()}`);

        } catch (error) {
            console.error(`Error: ${error}`);
            if (error instanceof Error && error.stack) {
                console.error(`Stack trace:\n${error.stack}`);
            }
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

        const allDocuments = Array.from(services.shared.workspace.LangiumDocuments.all) as LangiumDocument[];
        for (const document of allDocuments) {
            const model = document.parseResult?.value as Model | undefined;
            if (model?.aggregates) {
                for (const aggregate of model.aggregates) {
                    initializeAggregateProperties(aggregate);
                }
            }
        }

        const allValidationErrors: Array<{ file: string; error: any; document: LangiumDocument }> = [];

        for (const document of allDocuments) {
            const validationErrors = (document.diagnostics ?? []).filter((e: any) => e.severity === 1);
            if (validationErrors.length > 0) {
                const filePath = document.uri.fsPath || document.uri.path;
                for (const error of validationErrors) {
                    allValidationErrors.push({ file: filePath, error, document });
                }
            }
        }

        if (allValidationErrors.length > 0) {
            console.error('There are validation errors:');
            for (const { file, error, document } of allValidationErrors) {
                const relativePath = path.relative(process.cwd(), file);
                const lineNum = error.range.start.line + 1;
                const colNum = error.range.start.character + 1;
                const errorText = document.textDocument?.getText(error.range) || '';
                console.error(
                    `${relativePath}:${lineNum}:${colNum} - ${error.message}${errorText ? ` [${errorText}]` : ''}`
                );
            }
            process.exit(1);
        }
    }

    private static async setupConfiguration(opts: TemplateGenerateOptions, inputPath: string) {
        // Smart defaults - always generate everything
        const baseOutputDir = opts.destination || DEFAULT_OUTPUT_DIR;
        const projectName = opts.name || ProjectSetup.deriveProjectName(inputPath);
        const validate = opts.validate || true;

        // Load additional config from file if exists
        const loadedConfig = await ConfigLoader.loadConfig(inputPath, {
            projectName,
            outputDirectory: baseOutputDir
        });

        return {
            baseOutputDir,
            projectName,
            validate,
            basePackage: loadedConfig.basePackage,
            consistencyModels: loadedConfig.consistencyModels
        };
    }

    private static async parseModels(nebulaFiles: string[], services: any): Promise<Model[]> {
        const allModels: Model[] = [];

        for (const filePath of nebulaFiles) {
            const model = await extractAstNode<Model>(filePath, services);
            for (const aggregate of model.aggregates) {
                initializeAggregateProperties(aggregate);
            }
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
                    projectName: config.projectName
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




}
