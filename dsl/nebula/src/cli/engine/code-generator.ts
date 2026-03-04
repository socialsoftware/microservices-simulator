import chalk from "chalk";
import { Model, Aggregate } from "../../language/generated/ast.js";
import { createNebulaServices } from "../../language/nebula-module.js";
import { extractAstNode } from "../utils/cli-util.js";
import { collectNebulaFiles } from "../utils/file-utils.js";
import { NodeFileSystem } from "langium/node";
import { URI, type LangiumDocument } from "langium";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { initializeAggregateProperties, registerAllModels } from "../utils/aggregate-helpers.js";
import { FileWriter } from "../utils/file-writer.js";

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
import { DtoSchemaService } from "../services/dto-schema-service.js";
import { ServiceMappingGenerator } from "../generators/microservices/service/service-mapping-generator.js";
import { CommandGenerator } from "../generators/microservices/command/command-generator.js";
import { CommandHandlerGenerator } from "../generators/microservices/command/command-handler-generator.js";

export class CodeGenerator {


    static async generateCode(inputPath: string, opts: TemplateGenerateOptions): Promise<void> {
        try {
            console.log(`Starting generation for: ${inputPath}`);






            const nebulaFiles = await collectNebulaFiles(inputPath);
            if (nebulaFiles.length === 0) {
                console.error(`No Nebula files found at path: ${inputPath}`);
                process.exit(1);
            }
            console.log(`Found ${nebulaFiles.length} Nebula files`);

            const services = createNebulaServices(NodeFileSystem).nebulaServices;
            await this.loadLanguageDocuments(services, nebulaFiles);
            const models = await this.parseModels(nebulaFiles, services);


            registerAllModels(models);






            const config = await this.setupConfiguration(opts, inputPath);
            const paths = await ProjectSetup.setupProjectPaths(config.baseOutputDir, inputPath, config.projectName);


            try {
                await fs.rm(paths.projectPath, { recursive: true, force: true });
            } catch {
            }


            await this.validateModels(models, config);





            const generators = GeneratorRegistryFactory.createRegistry();

            const dtoSchemaService = new DtoSchemaService();
            const dtoSchemaRegistry = dtoSchemaService.buildFromModels(models);

            const options: GenerationOptions = {
                projectName: config.projectName,
                outputPath: paths.projectPath,
                basePackage: config.basePackage || getGlobalConfig().getBasePackage(),
                consistencyModels: config.consistencyModels,
                allModels: models,
                dtoSchemaRegistry
            };

            const aggregates = models.flatMap(model => model.aggregates);





            console.log("\nGenerating code...");
            for (const model of models) {
                for (const aggregate of model.aggregates) {
                    initializeAggregateProperties(aggregate);

                    FileWriter.resetCount();
                    const aggregatePath = paths.javaPath + '/microservices/' + aggregate.name.toLowerCase();

                    await EntityFeature.generateCoreComponents(aggregate, aggregatePath, options, generators);
                    await ServiceFeature.generateService(aggregate, aggregatePath, options, generators);
                    await EventsFeature.generateEvents(aggregate, aggregatePath, options, generators);
                    await CoordinationFeature.generateCoordination(aggregate, aggregatePath, options, generators, aggregates);
                    await WebApiFeature.generateWebApi(aggregate, aggregatePath, options, generators, aggregates);
                    await ValidationFeature.generateValidation(aggregate, paths, options, generators);
                    await SagaFeature.generateSaga(aggregate, aggregatePath, options, generators, aggregates);

                    const commandGenerator = new CommandGenerator();
                    const commandFiles = commandGenerator.generate(aggregate, options);
                    const commandPath = paths.javaPath + '/command/' + aggregate.name.toLowerCase();
                    for (const [fileName, content] of Object.entries(commandFiles)) {
                        await fs.mkdir(commandPath, { recursive: true });
                        await fs.writeFile(path.join(commandPath, fileName), content, 'utf-8');
                    }

                    const commandHandlerGenerator = new CommandHandlerGenerator();
                    const handlerFiles = commandHandlerGenerator.generate(aggregate, options);
                    const handlerPath = aggregatePath + '/commandHandler';
                    for (const [fileName, content] of Object.entries(handlerFiles)) {
                        await fs.mkdir(handlerPath, { recursive: true });
                        await fs.writeFile(path.join(handlerPath, fileName), content, 'utf-8');
                    }

                    const count = FileWriter.getCount();
                    const name = aggregate.name.padEnd(20);
                    console.log(`  ${name}${count} files`);
                }
            }





            const projectOptions: GenerationOptions = {
                projectName: config.projectName,
                outputPath: paths.projectPath,
                basePackage: options.basePackage
            };


            if (models[0]?.aggregates?.length > 0) {
                const integrationCode = await generators.integrationGenerator.generateIntegration(
                    models[0].aggregates[0],
                    projectOptions
                );
                const capitalizedProjectName = config.projectName.charAt(0).toUpperCase() + config.projectName.slice(1);
                const integrationPath = path.join(paths.javaPath, `${capitalizedProjectName}Simulator.java`);
                await fs.writeFile(integrationPath, integrationCode['application'], 'utf-8');

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

            await WebApiFeature.generateGlobalWebApi(paths, projectOptions, generators);

            const serviceMappingGenerator = new ServiceMappingGenerator();
            const serviceMappingCode = serviceMappingGenerator.generate(aggregates, projectOptions);
            const serviceMappingPath = path.join(paths.javaPath, 'ServiceMapping.java');
            await fs.writeFile(serviceMappingPath, serviceMappingCode, 'utf-8');

            const pomContent = TemplateGenerators.generatePomXml(config.projectName);
            await fs.writeFile(path.join(paths.projectPath, "pom.xml"), pomContent, 'utf-8');

            const gitignoreContent = TemplateGenerators.generateGitignore();
            await fs.writeFile(path.join(paths.projectPath, ".gitignore"), gitignoreContent, 'utf-8');

            try {
                await generators.configurationGenerator.generate({
                    projectName: config.projectName,
                    basePackage: config.basePackage || getGlobalConfig().getBasePackage(),
                    outputDirectory: paths.projectPath
                }, {
                    projectName: config.projectName
                });
            } catch (error) {
                console.warn(chalk.yellow(`[WARN] Configuration generation failed: ${error instanceof Error ? error.message : String(error)}`));
            }

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
            }

            const sharedCount = Object.keys(sharedResults).length;
            console.log(`\nGenerated project files (integration, pom.xml, .gitignore${sharedCount > 0 ? `, ${sharedCount} shared enums` : ''})`);

            console.log(chalk.green(`\nCode generation completed successfully!`));
            console.log(`Output: ${paths.projectPath}`);

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

        const baseOutputDir = opts.destination || DEFAULT_OUTPUT_DIR;
        const projectName = opts.name || ProjectSetup.deriveProjectName(inputPath);
        const validate = opts.validate || true;


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
        process.stdout.write("Validating DSL files... ");
        const generators = GeneratorRegistryFactory.createRegistry();

        const allAggregates: Aggregate[] = [];
        for (const model of allModels) {
            allAggregates.push(...model.aggregates);
        }

        const validationResult = await generators.validationSystem.validateAggregates(allAggregates, {
            projectName: config.projectName
        });

        if (!validationResult.isValid) {
            console.log(chalk.red("FAILED"));
            console.error(generators.validationSystem.getValidationReport(validationResult));
            process.exit(1);
        }

        console.log(chalk.green("OK"));
    }




}
