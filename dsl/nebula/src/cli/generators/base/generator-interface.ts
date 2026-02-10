import { Aggregate } from "../../../language/generated/ast.js";
import { GeneratorCapabilities } from "../common/generator-capabilities.js";

/**
 * Options passed to all generators
 */
export interface GenerationOptions {
    projectName: string;
    architecture?: string;
    outputDirectory?: string;
    transactionModel?: 'sagas' | 'tcc';
}

/**
 * Result of code generation
 */
export interface GenerationResult {
    files: GeneratedFile[];
    warnings: string[];
    errors: string[];
    metadata?: GenerationMetadata;
}

/**
 * A single generated file
 */
export interface GeneratedFile {
    path: string;
    content: string;
    description: string;
    category?: 'entity' | 'service' | 'repository' | 'dto' | 'event' | 'controller' | 'saga' | 'config';
}

/**
 * Metadata about the generation process
 */
export interface GenerationMetadata {
    aggregateName: string;
    generatorName: string;
    timestamp: string;
    duration?: number;
    linesGenerated?: number;
}

/**
 * Metadata about a generator itself
 */
export interface GeneratorMetadata {
    name: string;
    category: 'microservices' | 'coordination' | 'validation' | 'sagas' | 'config';
    dependencies: string[];
    version: string;
    description?: string;
}

/**
 * Standard interface that all generators should implement
 */
export interface Generator {
    /**
     * Generate code for an aggregate
     */
    generate(
        aggregate: Aggregate,
        options: GenerationOptions
    ): Promise<GenerationResult>;

    /**
     * Check if this generator can handle the given aggregate
     */
    canGenerate(aggregate: Aggregate): boolean;

    /**
     * Get metadata about this generator
     */
    getMetadata(): GeneratorMetadata;
}

/**
 * Abstract base class implementing common functionality
 * All generators should extend this to get standard behavior
 */
export abstract class BaseGenerator implements Generator {
    constructor(protected capabilities?: GeneratorCapabilities) {}

    /**
     * Subclasses must implement this
     */
    abstract generate(
        aggregate: Aggregate,
        options: GenerationOptions
    ): Promise<GenerationResult>;

    /**
     * Default implementation - can be overridden
     */
    canGenerate(aggregate: Aggregate): boolean {
        return true;
    }

    /**
     * Subclasses must implement this
     */
    abstract getMetadata(): GeneratorMetadata;

    /**
     * Helper to create a successful result
     */
    protected createResult(files: GeneratedFile[], warnings: string[] = [], errors: string[] = []): GenerationResult {
        return {
            files,
            warnings,
            errors,
            metadata: {
                aggregateName: '',
                generatorName: this.getMetadata().name,
                timestamp: new Date().toISOString(),
                linesGenerated: files.reduce((sum, f) => sum + f.content.split('\n').length, 0)
            }
        };
    }

    /**
     * Helper to create an error result
     */
    protected createErrorResult(error: string): GenerationResult {
        return {
            files: [],
            warnings: [],
            errors: [error]
        };
    }

    /**
     * Helper to create a file entry
     */
    protected createFile(
        path: string,
        content: string,
        description: string,
        category?: GeneratedFile['category']
    ): GeneratedFile {
        return {
            path,
            content,
            description,
            category
        };
    }
}
