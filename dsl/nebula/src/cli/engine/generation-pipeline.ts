


import chalk from 'chalk';
import { Aggregate } from "../../language/generated/ast.js";
import { GenerationOptions, GeneratorRegistry } from "./types.js";
import { FileWriter } from "../utils/file-writer.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../utils/error-handler.js";



export interface GeneratedContent {
    content: string;
    filePath: string;
    description: string;
    metadata?: Record<string, any>;
}



export interface GenerationResult {
    success: boolean;
    generatedFiles: GeneratedContent[];
    errors: string[];
    warnings: string[];
    metadata?: Record<string, any>;
}



export interface ValidationResult {
    isValid: boolean;
    errors: string[];
    warnings: string[];
}



export interface GenerationPipeline {
    

    generate(aggregate: Aggregate, options: GenerationOptions): Promise<GenerationResult>;

    

    validate(result: GenerationResult): Promise<ValidationResult>;

    

    write(result: GenerationResult): Promise<void>;

    

    getName(): string;
}



export abstract class BaseGenerationPipeline implements GenerationPipeline {
    protected name: string;

    constructor(name: string) {
        this.name = name;
    }

    abstract generate(aggregate: Aggregate, options: GenerationOptions): Promise<GenerationResult>;

    async validate(result: GenerationResult): Promise<ValidationResult> {
        
        return {
            isValid: result.success && result.errors.length === 0,
            errors: result.errors,
            warnings: result.warnings
        };
    }

    async write(result: GenerationResult): Promise<void> {
        if (!result.success) {
            throw new Error(`Cannot write failed generation result for ${this.name}`);
        }

        
        const writePromises = result.generatedFiles.map(file =>
            FileWriter.writeGeneratedFile(file.filePath, file.content, file.description)
        );

        await Promise.all(writePromises);
    }

    getName(): string {
        return this.name;
    }

    

    protected createSuccessResult(files: GeneratedContent[], metadata?: Record<string, any>): GenerationResult {
        return {
            success: true,
            generatedFiles: files,
            errors: [],
            warnings: [],
            metadata
        };
    }

    

    protected createErrorResult(errors: string[], warnings: string[] = []): GenerationResult {
        return {
            success: false,
            generatedFiles: [],
            errors,
            warnings
        };
    }
}



export class FeatureOrchestrator {
    private pipelines: GenerationPipeline[] = [];
    private parallelExecution: boolean;

    constructor(parallelExecution: boolean = true) {
        this.parallelExecution = parallelExecution;
    }

    

    addPipeline(pipeline: GenerationPipeline): this {
        this.pipelines.push(pipeline);
        return this;
    }

    

    addPipelines(pipelines: GenerationPipeline[]): this {
        this.pipelines.push(...pipelines);
        return this;
    }

    

    async execute(aggregate: Aggregate, options: GenerationOptions): Promise<OrchestrationResult> {

        const results: PipelineResult[] = [];

        if (this.parallelExecution) {
            results.push(...await this.executeInParallel(aggregate, options));
        } else {
            results.push(...await this.executeSequentially(aggregate, options));
        }

        return this.summarizeResults(results, aggregate.name);
    }

    

    private async executeInParallel(aggregate: Aggregate, options: GenerationOptions): Promise<PipelineResult[]> {
        const pipelinePromises = this.pipelines.map(pipeline =>
            this.executeSinglePipeline(pipeline, aggregate, options)
        );

        return Promise.all(pipelinePromises);
    }

    

    private async executeSequentially(aggregate: Aggregate, options: GenerationOptions): Promise<PipelineResult[]> {
        const results: PipelineResult[] = [];

        for (const pipeline of this.pipelines) {
            const result = await this.executeSinglePipeline(pipeline, aggregate, options);
            results.push(result);

            
            if (!result.generationResult.success) {
                console.warn(chalk.yellow(`[WARN] Pipeline ${pipeline.getName()} failed, continuing with remaining pipelines`));
            }
        }

        return results;
    }

    

    private async executeSinglePipeline(
        pipeline: GenerationPipeline,
        aggregate: Aggregate,
        options: GenerationOptions
    ): Promise<PipelineResult> {
        const pipelineName = pipeline.getName();

        const result = await ErrorHandler.wrapAsync(
            async () => {

                
                const generationResult = await pipeline.generate(aggregate, options);

                if (!generationResult.success) {
                    return {
                        pipelineName,
                        generationResult,
                        validationResult: { isValid: false, errors: generationResult.errors, warnings: [] },
                        writeSuccess: false
                    };
                }

                
                const validationResult = await pipeline.validate(generationResult);

                if (!validationResult.isValid) {
                    return {
                        pipelineName,
                        generationResult,
                        validationResult,
                        writeSuccess: false
                    };
                }

                
                await pipeline.write(generationResult);


                return {
                    pipelineName,
                    generationResult,
                    validationResult,
                    writeSuccess: true
                };
            },
            ErrorUtils.aggregateContext(
                'execute pipeline',
                aggregate.name,
                'feature-orchestrator',
                { pipelineName }
            ),
            ErrorSeverity.ERROR
        );

        return result || {
            pipelineName,
            generationResult: { success: false, generatedFiles: [], errors: [`Pipeline ${pipelineName} failed`], warnings: [] },
            validationResult: { isValid: false, errors: [`Pipeline ${pipelineName} failed`], warnings: [] },
            writeSuccess: false
        };
    }

    

    private summarizeResults(results: PipelineResult[], aggregateName: string): OrchestrationResult {
        const successful = results.filter(r => r.writeSuccess);
        const failed = results.filter(r => !r.writeSuccess);
        const totalFiles = results.reduce((sum, r) => sum + r.generationResult.generatedFiles.length, 0);
        const allErrors = results.flatMap(r => r.generationResult.errors);
        const allWarnings = results.flatMap(r => r.generationResult.warnings);

        if (failed.length > 0) {
            console.error(chalk.red(`[ERROR] Failed pipelines for ${aggregateName}: ${failed.map(f => f.pipelineName).join(', ')}`));
        }
        if (allWarnings.length > 0) {
            console.warn(chalk.yellow(`[WARN] ${allWarnings.length} warning(s) for ${aggregateName}`));
        }

        return {
            aggregateName,
            totalPipelines: results.length,
            successfulPipelines: successful.length,
            failedPipelines: failed.length,
            totalFiles,
            errors: allErrors,
            warnings: allWarnings,
            pipelineResults: results
        };
    }

    

    clear(): this {
        this.pipelines = [];
        return this;
    }

    

    getPipelineCount(): number {
        return this.pipelines.length;
    }
}



export interface PipelineResult {
    pipelineName: string;
    generationResult: GenerationResult;
    validationResult: ValidationResult;
    writeSuccess: boolean;
}



export interface OrchestrationResult {
    aggregateName: string;
    totalPipelines: number;
    successfulPipelines: number;
    failedPipelines: number;
    totalFiles: number;
    errors: string[];
    warnings: string[];
    pipelineResults: PipelineResult[];
}






export class EntityPipeline extends BaseGenerationPipeline {
    constructor(private generators: GeneratorRegistry) {
        super('Entity');
    }

    async generate(aggregate: Aggregate, options: GenerationOptions): Promise<GenerationResult> {
        try {
            const files: GeneratedContent[] = [];

            
            for (const entity of aggregate.entities) {
                const entityOptions = {
                    projectName: options.projectName,
                    dtoSchemaRegistry: options.dtoSchemaRegistry,
                    allEntities: aggregate.entities
                };

                const entityCode = await this.generators.entityGenerator.generateEntity(entity, entityOptions);
                files.push({
                    content: entityCode,
                    filePath: this.buildEntityPath(aggregate, entity, options),
                    description: `entity ${entity.name}`,
                    metadata: { entityName: entity.name, isRoot: (entity as any).isRoot }
                });

                
                
                const dtoCode = await this.generators.dtoGenerator.generateDto(entity, options);
                files.push({
                    content: dtoCode,
                    filePath: this.buildDtoPath(aggregate, entity, options),
                    description: `DTO ${entity.name}Dto`,
                    metadata: { entityName: entity.name, type: 'dto' }
                });
            }

            
            const factoryCode = await this.generators.factoryGenerator.generateFactory(aggregate, {
                ...options,
                dtoSchemaRegistry: options.dtoSchemaRegistry
            });
            files.push({
                content: factoryCode,
                filePath: this.buildFactoryPath(aggregate, options),
                description: `factory ${aggregate.name}Factory`,
                metadata: { type: 'factory' }
            });

            
            const repositoryCode = await this.generators.repositoryGenerator.generateRepository(aggregate, options);
            files.push({
                content: repositoryCode,
                filePath: this.buildRepositoryPath(aggregate, options),
                description: `custom repository ${aggregate.name}CustomRepository`,
                metadata: { type: 'repository' }
            });

            const repositoryInterfaceCode = await this.generators.repositoryInterfaceGenerator.generateRepositoryInterface(aggregate, options);
            files.push({
                content: repositoryInterfaceCode,
                filePath: this.buildRepositoryInterfacePath(aggregate, options),
                description: `repository interface ${aggregate.name}Repository`,
                metadata: { type: 'repository-interface' }
            });

            return this.createSuccessResult(files, {
                entityCount: aggregate.entities.length,
                hasFactory: true,
                hasRepository: true
            });

        } catch (error) {
            return this.createErrorResult([`Entity generation failed: ${error instanceof Error ? error.message : String(error)}`]);
        }
    }

    private buildEntityPath(aggregate: Aggregate, entity: any, options: GenerationOptions): string {
        return `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/microservices/${aggregate.name.toLowerCase()}/aggregate/${entity.name}.java`;
    }

    private buildDtoPath(_aggregate: Aggregate, entity: any, options: GenerationOptions): string {
        return `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/shared/dtos/${entity.name}Dto.java`;
    }

    private buildFactoryPath(aggregate: Aggregate, options: GenerationOptions): string {
        return `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/microservices/${aggregate.name.toLowerCase()}/aggregate/${aggregate.name}Factory.java`;
    }

    private buildRepositoryPath(aggregate: Aggregate, options: GenerationOptions): string {
        return `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/microservices/${aggregate.name.toLowerCase()}/aggregate/${aggregate.name}CustomRepository.java`;
    }

    private buildRepositoryInterfacePath(aggregate: Aggregate, options: GenerationOptions): string {
        return `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/microservices/${aggregate.name.toLowerCase()}/aggregate/${aggregate.name}Repository.java`;
    }

}



export class ServicePipeline extends BaseGenerationPipeline {
    constructor(private generators: GeneratorRegistry) {
        super('Service');
    }

    async generate(aggregate: Aggregate, options: GenerationOptions): Promise<GenerationResult> {
        try {
            const files: GeneratedContent[] = [];
            const rootEntity = aggregate.entities.find((e: any) => e.isRoot);

            if (!rootEntity) {
                return this.createErrorResult([`No root entity found in aggregate ${aggregate.name}`]);
            }

            const serviceDefinition = (aggregate as any).serviceDefinition;
            if (serviceDefinition) {
                
                const serviceCode = await this.generators.serviceDefinitionGenerator.generateServiceFromDefinition(
                    aggregate,
                    rootEntity,
                    options
                );

                files.push({
                    content: serviceCode,
                    filePath: this.buildServicePath(aggregate, serviceDefinition, options),
                    description: `service ${serviceDefinition.name || aggregate.name + 'Service'}`,
                    metadata: { type: 'service', hasDefinition: true }
                });
            } else {
                
                const serviceCode = await this.generators.serviceGenerator.generateService(aggregate, options);
                files.push({
                    content: serviceCode,
                    filePath: this.buildDefaultServicePath(aggregate, options),
                    description: `default service ${aggregate.name}Service`,
                    metadata: { type: 'service', hasDefinition: false }
                });
            }

            return this.createSuccessResult(files, { hasServiceDefinition: !!serviceDefinition });

        } catch (error) {
            return this.createErrorResult([`Service generation failed: ${error instanceof Error ? error.message : String(error)}`]);
        }
    }

    private buildServicePath(aggregate: Aggregate, serviceDefinition: any, options: GenerationOptions): string {
        const serviceName = serviceDefinition.name || `${aggregate.name}Service`;
        return `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/microservices/${aggregate.name.toLowerCase()}/service/${serviceName}.java`;
    }

    private buildDefaultServicePath(aggregate: Aggregate, options: GenerationOptions): string {
        return `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/microservices/${aggregate.name.toLowerCase()}/service/${aggregate.name}Service.java`;
    }
}



export class EventPipeline extends BaseGenerationPipeline {
    constructor(private generators: GeneratorRegistry) {
        super('Event');
    }

    async generate(aggregate: Aggregate, options: GenerationOptions): Promise<GenerationResult> {
        try {
            const files: GeneratedContent[] = [];

            if (!aggregate.events) {
                return this.createSuccessResult([], { hasEvents: false });
            }

            const eventCode = await this.generators.eventGenerator.generateEvents(aggregate, options);

            
            for (const [key, content] of Object.entries(eventCode)) {
                if (typeof content === 'string') {
                    files.push({
                        content,
                        filePath: this.buildEventPath(aggregate, key, options),
                        description: this.buildEventDescription(key),
                        metadata: { type: 'event', eventKey: key }
                    });
                }
            }

            return this.createSuccessResult(files, {
                hasEvents: true,
                eventCount: files.length
            });

        } catch (error) {
            return this.createErrorResult([`Event generation failed: ${error instanceof Error ? error.message : String(error)}`]);
        }
    }

    private buildEventPath(aggregate: Aggregate, eventKey: string, options: GenerationOptions): string {
        const basePath = `${options.outputPath}/src/main/java/${options.basePackage.replace(/\./g, '/')}/${options.projectName.toLowerCase()}/microservices/${aggregate.name.toLowerCase()}/events`;

        if (eventKey === 'event-handling') {
            return `${basePath}/handling/${aggregate.name}EventHandling.java`;
        } else if (eventKey.startsWith('event-handler-')) {
            const handlerName = eventKey.replace('event-handler-', '');
            return `${basePath}/handling/handlers/${handlerName}.java`;
        } else if (eventKey.startsWith('published-event-')) {
            const eventName = eventKey.replace('published-event-', '');
            return `${basePath}/publish/${eventName}.java`;
        } else if (eventKey.startsWith('event-subscription-')) {
            const subscriptionName = eventKey.replace('event-subscription-', '');
            return `${basePath}/subscribe/Subscribes${subscriptionName}.java`;
        }

        return `${basePath}/${eventKey}.java`;
    }

    private buildEventDescription(eventKey: string): string {
        if (eventKey === 'event-handling') return 'event handling';
        if (eventKey.startsWith('event-handler-')) return `event handler ${eventKey.replace('event-handler-', '')}`;
        if (eventKey.startsWith('published-event-')) return `published event ${eventKey.replace('published-event-', '')}`;
        if (eventKey.startsWith('event-subscription-')) return `event subscription ${eventKey.replace('event-subscription-', '')}`;
        return eventKey;
    }
}



export class PipelineFactory {
    

    static createEntityPipeline(generators: GeneratorRegistry): EntityPipeline {
        return new EntityPipeline(generators);
    }

    

    static createServicePipeline(generators: GeneratorRegistry): ServicePipeline {
        return new ServicePipeline(generators);
    }

    

    static createEventPipeline(generators: GeneratorRegistry): EventPipeline {
        return new EventPipeline(generators);
    }

    

    static createStandardOrchestrator(generators: GeneratorRegistry, parallel: boolean = true): FeatureOrchestrator {
        return new FeatureOrchestrator(parallel)
            .addPipeline(this.createEntityPipeline(generators))
            .addPipeline(this.createServicePipeline(generators))
            .addPipeline(this.createEventPipeline(generators));
    }

    

    static createCustomOrchestrator(
        generators: GeneratorRegistry,
        pipelineTypes: ('entity' | 'service' | 'event')[],
        parallel: boolean = true
    ): FeatureOrchestrator {
        const orchestrator = new FeatureOrchestrator(parallel);

        pipelineTypes.forEach(type => {
            switch (type) {
                case 'entity':
                    orchestrator.addPipeline(this.createEntityPipeline(generators));
                    break;
                case 'service':
                    orchestrator.addPipeline(this.createServicePipeline(generators));
                    break;
                case 'event':
                    orchestrator.addPipeline(this.createEventPipeline(generators));
                    break;
            }
        });

        return orchestrator;
    }
}
