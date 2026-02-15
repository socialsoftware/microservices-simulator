


import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { ValidationGenerationOptions } from "./validation-types.js";
import { InvariantGenerator } from "./invariant-generator.js";
import { AnnotationGenerator } from "./annotation-generator.js";
import { CustomValidatorGenerator } from "./custom-validator-generator.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../utils/error-handler.js";
import { FileWriter } from "../../utils/file-writer.js";
import { TemplateContextBuilder } from "../common/template-context-builder.js";
import * as path from 'path';



export interface ValidationOrchestrationConfig {
    generateInvariants: boolean;
    generateAnnotations: boolean;
    generateCustomValidators: boolean;
    outputPath: string;
    useUnifiedContext: boolean;
}



export interface UnifiedValidationContext {
    
    projectName: string;
    aggregateName: string;
    capitalizedAggregate: string;
    lowerAggregate: string;
    packageName: string;
    rootEntityName: string;

    
    invariants?: any[];
    invariantMethods?: any[];

    
    annotations?: any[];
    validationAnnotations?: any[];

    
    customValidators?: any[];
    validators?: any[];

    
    imports: string[];
    properties: any[];
}



export class ValidationOrchestrator {
    private invariantGenerator: InvariantGenerator;
    private annotationGenerator: AnnotationGenerator;
    private customValidatorGenerator: CustomValidatorGenerator;

    constructor() {
        this.invariantGenerator = new InvariantGenerator();
        this.annotationGenerator = new AnnotationGenerator();
        this.customValidatorGenerator = new CustomValidatorGenerator();
    }

    

    async generateValidation(
        aggregate: Aggregate,
        options: ValidationGenerationOptions,
        config: ValidationOrchestrationConfig = this.getDefaultConfig(options)
    ): Promise<{ [key: string]: string }> {

        const result = await ErrorHandler.wrapAsync(
            async () => this.generateValidationInternal(aggregate, options, config),
            ErrorUtils.aggregateContext(
                'generate validation',
                aggregate.name,
                'validation-orchestrator',
                { config }
            ),
            ErrorSeverity.ERROR
        );

        return result || {};
    }

    

    async generateAndWriteValidation(
        aggregate: Aggregate,
        outputPath: string,
        options: ValidationGenerationOptions,
        config?: ValidationOrchestrationConfig
    ): Promise<void> {
        const validationConfig = config || { ...this.getDefaultConfig(options), outputPath };
        const results = await this.generateValidation(aggregate, options, validationConfig);

        
        await this.writeValidationFiles(results, aggregate, outputPath, options);
    }

    

    private async generateValidationInternal(
        aggregate: Aggregate,
        options: ValidationGenerationOptions,
        config: ValidationOrchestrationConfig
    ): Promise<{ [key: string]: string }> {
        const rootEntity = this.findRootEntity(aggregate);
        const results: { [key: string]: string } = {};

        
        if (config.generateInvariants) {
            results['invariants'] = await this.invariantGenerator.generateInvariants(aggregate, rootEntity, options);
        }

        
        if (config.generateAnnotations) {
            results['annotations'] = await this.annotationGenerator.generateValidationAnnotations(aggregate, rootEntity, options);
        }

        
        if (config.generateCustomValidators) {
            const validators = await this.customValidatorGenerator.generateCustomValidators(aggregate, rootEntity, options);
            Object.assign(results, validators);
        }

        return results;
    }

    

    createUnifiedValidationContext(
        aggregate: Aggregate,
        rootEntity: Entity,
        options: ValidationGenerationOptions
    ): UnifiedValidationContext {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        return TemplateContextBuilder.create()
            .forValidation(options.projectName, aggregate, 'invariants')
            .withCustomData('rootEntityName', rootEntity.name)
            .withCustomData('capitalizedAggregate', capitalizedAggregate)
            .withCustomData('lowerAggregate', lowerAggregate)
            .withProperties(rootEntity)
            .buildPartial() as UnifiedValidationContext;
    }

    

    private async writeValidationFiles(
        results: { [key: string]: string },
        aggregate: Aggregate,
        outputPath: string,
        options: ValidationGenerationOptions
    ): Promise<void> {
        const javaPath = path.join(outputPath, 'src', 'main', 'java', 'pt', 'ulisboa', 'tecnico', 'socialsoftware', options.projectName.toLowerCase());

        
        if (results['invariants']) {
            const invariantsPath = path.join(javaPath, 'coordination', 'validation', `${aggregate.name}Invariants.java`);
            await FileWriter.writeGeneratedFile(invariantsPath, results['invariants'], `validation ${aggregate.name}Invariants`);
        }

        
        if (results['annotations']) {
            const annotationsPath = path.join(javaPath, 'coordination', 'validation', `${aggregate.name}ValidationAnnotations.java`);
            await FileWriter.writeGeneratedFile(annotationsPath, results['annotations'], `validation annotations ${aggregate.name}ValidationAnnotations`);
        }

        
        for (const [key, content] of Object.entries(results)) {
            if (key !== 'invariants' && key !== 'annotations' && typeof content === 'string') {
                const validatorPath = path.join(javaPath, 'coordination', 'validation', key);
                await FileWriter.writeGeneratedFile(validatorPath, content, `validator ${key}`);
            }
        }
    }

    

    async generateValidationForAggregates(
        aggregates: Aggregate[],
        outputPath: string,
        options: ValidationGenerationOptions
    ): Promise<void> {
        const generatePromises = aggregates.map(aggregate =>
            this.generateAndWriteValidation(aggregate, outputPath, options)
        );

        await Promise.all(generatePromises);
        console.log(`📝 Generated validation for ${aggregates.length} aggregates`);
    }

    

    private findRootEntity(aggregate: Aggregate): Entity {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            ErrorHandler.handle(
                new Error(`No root entity found in aggregate ${aggregate.name}`),
                ErrorUtils.aggregateContext(
                    'find root entity for validation',
                    aggregate.name,
                    'validation-orchestrator'
                ),
                ErrorSeverity.FATAL
            );
            throw new Error('This will never be reached'); 
        }
        return rootEntity;
    }

    private capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    private getDefaultConfig(options: ValidationGenerationOptions): ValidationOrchestrationConfig {
        return {
            generateInvariants: true,
            generateAnnotations: true,
            generateCustomValidators: true,
            outputPath: './output', 
            useUnifiedContext: false 
        };
    }

    

    static async generateValidation(aggregate: Aggregate, options: ValidationGenerationOptions): Promise<{ [key: string]: string }> {
        const orchestrator = new ValidationOrchestrator();
        return orchestrator.generateValidation(aggregate, options);
    }

    static async generateValidationWithFiles(
        aggregate: Aggregate,
        outputPath: string,
        options: ValidationGenerationOptions
    ): Promise<void> {
        const orchestrator = new ValidationOrchestrator();
        await orchestrator.generateAndWriteValidation(aggregate, outputPath, options);
    }
}



export class ValidationOrchestratorFactory {
    

    static forInvariants(): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        
        return orchestrator;
    }

    

    static forAnnotations(): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        
        return orchestrator;
    }

    

    static forCustomValidators(): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        
        return orchestrator;
    }

    

    static withConfig(config: Partial<ValidationOrchestrationConfig>): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        
        return orchestrator;
    }
}
