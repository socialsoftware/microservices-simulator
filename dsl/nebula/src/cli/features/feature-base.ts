/**
 * Abstract base class for all feature generators
 * 
 * Consolidates common functionality shared across feature classes including:
 * - Directory creation with consistent error handling
 * - File writing patterns
 * - Logging and error reporting
 * - Path management utilities
 */

import * as path from 'node:path';
import { GenerationOptions } from '../engine/types.js';
import { FileWriter } from '../utils/file-writer.js';
import { ErrorHandler, ErrorUtils, ErrorSeverity } from '../utils/error-handler.js';

/**
 * Common interface for feature generation results
 */
export interface FeatureResult {
    success: boolean;
    filesGenerated: number;
    errors: string[];
    warnings: string[];
}

/**
 * Abstract base class providing common functionality for all features
 */
export abstract class FeatureBase {
    protected featureName: string;

    constructor(featureName: string) {
        this.featureName = featureName;
    }

    /**
     * Abstract method that each feature must implement
     */
    abstract generate(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators?: any
    ): Promise<FeatureResult>;

    /**
     * Create a directory with consistent error handling and logging
     * @param dirPath - The directory path to create
     * @param description - Description for logging (e.g., "entity directory", "service directory")
     */
    protected async createDirectory(dirPath: string, description?: string): Promise<void> {
        try {
            await FileWriter.ensureDirectory(dirPath);
            if (description) {
                console.log(`\t- Created ${description}: ${path.basename(dirPath)}`);
            }
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.fileContext('create directory', dirPath, {
                    feature: this.featureName,
                    description
                }),
                ErrorSeverity.ERROR
            );
            throw error;
        }
    }

    /**
     * Write a generated file with consistent logging and error handling
     * @param filePath - The file path to write to
     * @param content - The content to write
     * @param description - Description for logging
     */
    protected async writeGeneratedFile(
        filePath: string,
        content: string,
        description: string
    ): Promise<void> {
        try {
            await FileWriter.writeGeneratedFile(filePath, content, description);
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.fileContext('write generated file', filePath, {
                    feature: this.featureName,
                    description
                }),
                ErrorSeverity.ERROR
            );
            throw error;
        }
    }

    /**
     * Write multiple files with batch processing
     * @param files - Map of file paths to content
     * @param basePath - Optional base path for relative files
     * @param logPrefix - Optional prefix for log messages
     */
    protected async writeMultipleFiles(
        files: Map<string, string>,
        basePath?: string,
        logPrefix?: string
    ): Promise<void> {
        try {
            await FileWriter.writeMultipleFiles(files, basePath, logPrefix || this.featureName);
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.aggregateContext(
                    'write multiple files',
                    'batch',
                    this.featureName,
                    { fileCount: files.size, basePath }
                ),
                ErrorSeverity.ERROR
            );
            throw error;
        }
    }

    /**
     * Write files from a generator result object (common pattern)
     * @param generatorResult - Object with file keys and content values
     * @param pathBuilder - Function to build file path from key
     * @param descriptionBuilder - Function to build description from key
     */
    protected async writeFilesFromGeneratorResult(
        generatorResult: { [key: string]: string },
        pathBuilder: (key: string) => string,
        descriptionBuilder: (key: string) => string
    ): Promise<void> {
        try {
            await FileWriter.writeFilesFromObject(
                generatorResult,
                pathBuilder,
                descriptionBuilder
            );
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.aggregateContext(
                    'write generator result files',
                    'batch',
                    this.featureName,
                    { fileCount: Object.keys(generatorResult).length }
                ),
                ErrorSeverity.ERROR
            );
            throw error;
        }
    }

    /**
     * Build a standard Java package path
     * @param basePath - Base path (e.g., paths.javaPath)
     * @param packageSegments - Package segments (e.g., ['coordination', 'validation'])
     * @returns Full directory path
     */
    protected buildPackagePath(basePath: string, ...packageSegments: string[]): string {
        return path.join(basePath, ...packageSegments);
    }

    /**
     * Build a standard file path for Java classes
     * @param basePath - Base path
     * @param packageSegments - Package segments
     * @param className - Class name (without .java extension)
     * @returns Full file path
     */
    protected buildJavaFilePath(
        basePath: string,
        packageSegments: string[],
        className: string
    ): string {
        return path.join(basePath, ...packageSegments, `${className}.java`);
    }

    /**
     * Log feature start
     * @param aggregateName - Name of the aggregate being processed
     */
    protected logFeatureStart(aggregateName: string): void {
        console.log(`\t🔧 Generating ${this.featureName} for ${aggregateName}...`);
    }

    /**
     * Log feature completion
     * @param aggregateName - Name of the aggregate processed
     * @param filesGenerated - Number of files generated
     */
    protected logFeatureComplete(aggregateName: string, filesGenerated: number): void {
        console.log(`\t✅ ${this.featureName} generation complete (${filesGenerated} files)`);
    }

    /**
     * Log feature warning
     * @param message - Warning message
     * @param context - Additional context
     */
    protected logWarning(message: string, context?: any): void {
        console.warn(`\t⚠️  ${this.featureName}: ${message}`);
        if (context) {
            console.warn(`\t   Context: ${JSON.stringify(context)}`);
        }
    }

    /**
     * Handle feature-specific errors with consistent logging
     * @param error - The error that occurred
     * @param operation - The operation that failed
     * @param context - Additional context
     */
    protected handleFeatureError(
        error: Error,
        operation: string,
        context?: any
    ): void {
        ErrorHandler.handle(
            error,
            ErrorUtils.aggregateContext(
                operation,
                context?.aggregate || 'unknown',
                this.featureName,
                context
            ),
            ErrorSeverity.ERROR
        );
    }

    /**
     * Create a successful feature result
     * @param filesGenerated - Number of files generated
     * @param warnings - Optional warnings
     */
    protected createSuccessResult(
        filesGenerated: number,
        warnings: string[] = []
    ): FeatureResult {
        return {
            success: true,
            filesGenerated,
            errors: [],
            warnings
        };
    }

    /**
     * Create a failed feature result
     * @param errors - Error messages
     * @param filesGenerated - Number of files that were generated before failure
     */
    protected createErrorResult(
        errors: string[],
        filesGenerated: number = 0
    ): FeatureResult {
        return {
            success: false,
            filesGenerated,
            errors,
            warnings: []
        };
    }

    /**
     * Wrap feature generation with consistent error handling and logging
     * @param aggregateName - Name of the aggregate
     * @param operation - The generation operation to perform
     */
    protected async executeFeatureGeneration(
        aggregateName: string,
        operation: () => Promise<FeatureResult>
    ): Promise<FeatureResult> {
        this.logFeatureStart(aggregateName);

        try {
            const result = await operation();

            if (result.success) {
                this.logFeatureComplete(aggregateName, result.filesGenerated);
            } else {
                console.error(`\t❌ ${this.featureName} generation failed for ${aggregateName}`);
                result.errors.forEach(error => console.error(`\t   Error: ${error}`));
            }

            result.warnings.forEach(warning => this.logWarning(warning));

            return result;
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);
            console.error(`\t❌ ${this.featureName} generation failed for ${aggregateName}: ${errorMessage}`);

            this.handleFeatureError(
                error instanceof Error ? error : new Error(errorMessage),
                'feature generation',
                { aggregate: aggregateName }
            );

            return this.createErrorResult([errorMessage]);
        }
    }

    /**
     * Utility method to check if a generator exists and is callable
     * @param generators - Generator registry
     * @param generatorName - Name of the generator to check
     */
    protected hasGenerator(generators: any, generatorName: string): boolean {
        return generators &&
            generators[generatorName] &&
            typeof generators[generatorName].generate === 'function';
    }

    /**
     * Safely call a generator with error handling
     * @param generator - The generator to call
     * @param args - Arguments to pass to the generator
     * @param generatorName - Name for error reporting
     */
    protected async safeGeneratorCall(
        generator: any,
        args: any[],
        generatorName: string
    ): Promise<any> {
        try {
            if (typeof generator.generate === 'function') {
                return await generator.generate(...args);
            } else if (typeof generator === 'function') {
                return await generator(...args);
            } else {
                throw new Error(`Generator ${generatorName} is not callable`);
            }
        } catch (error) {
            this.handleFeatureError(
                error instanceof Error ? error : new Error(String(error)),
                `call ${generatorName} generator`,
                { generatorName, argsCount: args.length }
            );
            throw error;
        }
    }
}
