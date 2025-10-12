/**
 * Unified Validation Generation Orchestrator
 * 
 * This module consolidates all validation generation logic from InvariantGenerator,
 * AnnotationGenerator, and CustomValidatorGenerator into a single, comprehensive
 * orchestration system that eliminates duplicate validation patterns.
 */

import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { ValidationGenerationOptions } from "./validation-types.js";
import { InvariantGenerator } from "./invariant-generator.js";
import { AnnotationGenerator } from "./annotation-generator.js";
import { CustomValidatorGenerator } from "./custom-validator-generator.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../utils/error-handler.js";
import { FileWriter } from "../../utils/file-writer.js";
import { TemplateContextBuilder } from "../common/template-context-builder.js";
import * as path from 'path';

/**
 * Validation generation configuration
 */
export interface ValidationOrchestrationConfig {
    generateInvariants: boolean;
    generateAnnotations: boolean;
    generateCustomValidators: boolean;
    outputPath: string;
    useUnifiedContext: boolean;
}

/**
 * Unified validation context that combines all validation contexts
 */
export interface UnifiedValidationContext {
    // Base context
    projectName: string;
    aggregateName: string;
    capitalizedAggregate: string;
    lowerAggregate: string;
    packageName: string;
    rootEntityName: string;

    // Invariant-specific
    invariants?: any[];
    invariantMethods?: any[];

    // Annotation-specific
    annotations?: any[];
    validationAnnotations?: any[];

    // Custom validator-specific
    customValidators?: any[];
    validators?: any[];

    // Common
    imports: string[];
    properties: any[];
}

/**
 * Unified validation orchestrator that coordinates all validation generation
 */
export class ValidationOrchestrator {
    private invariantGenerator: InvariantGenerator;
    private annotationGenerator: AnnotationGenerator;
    private customValidatorGenerator: CustomValidatorGenerator;

    constructor() {
        this.invariantGenerator = new InvariantGenerator();
        this.annotationGenerator = new AnnotationGenerator();
        this.customValidatorGenerator = new CustomValidatorGenerator();
    }

    /**
     * Generate all validation components for an aggregate
     */
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

    /**
     * Generate validation with file writing
     */
    async generateAndWriteValidation(
        aggregate: Aggregate,
        outputPath: string,
        options: ValidationGenerationOptions,
        config?: ValidationOrchestrationConfig
    ): Promise<void> {
        const validationConfig = config || { ...this.getDefaultConfig(options), outputPath };
        const results = await this.generateValidation(aggregate, options, validationConfig);

        // Write validation files
        await this.writeValidationFiles(results, aggregate, outputPath, options);
    }

    /**
     * Internal validation generation logic
     */
    private async generateValidationInternal(
        aggregate: Aggregate,
        options: ValidationGenerationOptions,
        config: ValidationOrchestrationConfig
    ): Promise<{ [key: string]: string }> {
        const rootEntity = this.findRootEntity(aggregate);
        const results: { [key: string]: string } = {};

        // Generate invariants
        if (config.generateInvariants) {
            results['invariants'] = await this.invariantGenerator.generateInvariants(aggregate, rootEntity, options);
        }

        // Generate annotations
        if (config.generateAnnotations) {
            results['annotations'] = await this.annotationGenerator.generateValidationAnnotations(aggregate, rootEntity, options);
        }

        // Generate custom validators
        if (config.generateCustomValidators) {
            const validators = await this.customValidatorGenerator.generateCustomValidators(aggregate, rootEntity, options);
            Object.assign(results, validators);
        }

        return results;
    }

    /**
     * Create unified validation context (for future use)
     */
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

    /**
     * Write validation files to disk
     */
    private async writeValidationFiles(
        results: { [key: string]: string },
        aggregate: Aggregate,
        outputPath: string,
        options: ValidationGenerationOptions
    ): Promise<void> {
        const javaPath = path.join(outputPath, 'src', 'main', 'java', 'pt', 'ulisboa', 'tecnico', 'socialsoftware', options.projectName.toLowerCase());

        // Write invariants
        if (results['invariants']) {
            const invariantsPath = path.join(javaPath, 'coordination', 'validation', `${aggregate.name}Invariants.java`);
            await FileWriter.writeGeneratedFile(invariantsPath, results['invariants'], `validation ${aggregate.name}Invariants`);
        }

        // Write annotations
        if (results['annotations']) {
            const annotationsPath = path.join(javaPath, 'coordination', 'validation', `${aggregate.name}ValidationAnnotations.java`);
            await FileWriter.writeGeneratedFile(annotationsPath, results['annotations'], `validation annotations ${aggregate.name}ValidationAnnotations`);
        }

        // Write custom validators
        for (const [key, content] of Object.entries(results)) {
            if (key !== 'invariants' && key !== 'annotations' && typeof content === 'string') {
                const validatorPath = path.join(javaPath, 'coordination', 'validation', key);
                await FileWriter.writeGeneratedFile(validatorPath, content, `validator ${key}`);
            }
        }
    }

    /**
     * Batch generate validation for multiple aggregates
     */
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

    /**
     * Utility methods
     */
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
            throw new Error('This will never be reached'); // For TypeScript
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
            outputPath: './output', // Default output path
            useUnifiedContext: false // Keep false for backward compatibility
        };
    }

    /**
     * Static convenience methods for backward compatibility
     */
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

/**
 * Validation generation factory for creating orchestrators with specific configurations
 */
export class ValidationOrchestratorFactory {
    /**
     * Create orchestrator for invariant generation only
     */
    static forInvariants(): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        // Could customize the orchestrator here if needed
        return orchestrator;
    }

    /**
     * Create orchestrator for annotation generation only
     */
    static forAnnotations(): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        // Could customize the orchestrator here if needed
        return orchestrator;
    }

    /**
     * Create orchestrator for custom validators only
     */
    static forCustomValidators(): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        // Could customize the orchestrator here if needed
        return orchestrator;
    }

    /**
     * Create orchestrator with custom configuration
     */
    static withConfig(config: Partial<ValidationOrchestrationConfig>): ValidationOrchestrator {
        const orchestrator = new ValidationOrchestrator();
        // Could apply custom configuration here
        return orchestrator;
    }
}
