


import * as path from 'node:path';
import chalk from 'chalk';
import { GenerationOptions } from '../engine/types.js';
import { FileWriter } from '../utils/file-writer.js';
import { ErrorHandler, ErrorUtils, ErrorSeverity } from '../utils/error-handler.js';



export interface FeatureResult {
    success: boolean;
    filesGenerated: number;
    errors: string[];
    warnings: string[];
}



export abstract class FeatureBase {
    protected featureName: string;

    constructor(featureName: string) {
        this.featureName = featureName;
    }

    

    abstract generate(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators?: any
    ): Promise<FeatureResult>;

    

    protected async createDirectory(dirPath: string, description?: string): Promise<void> {
        try {
            await FileWriter.ensureDirectory(dirPath);
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

    

    protected buildPackagePath(basePath: string, ...packageSegments: string[]): string {
        return path.join(basePath, ...packageSegments);
    }

    

    protected buildJavaFilePath(
        basePath: string,
        packageSegments: string[],
        className: string
    ): string {
        return path.join(basePath, ...packageSegments, `${className}.java`);
    }

    

    protected logFeatureStart(_aggregateName: string): void {
    }

    

    protected logFeatureComplete(_aggregateName: string, _filesGenerated: number): void {
    }

    

    protected logWarning(message: string, context?: any): void {
        console.warn(chalk.yellow(`[WARN] ${this.featureName}: ${message}`));
        if (context) {
            console.warn(chalk.yellow(`  Context: ${JSON.stringify(context)}`));
        }
    }

    

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
                console.error(chalk.red(`[ERROR] ${this.featureName} generation failed for ${aggregateName}`));
                result.errors.forEach(error => console.error(chalk.red(`  ${error}`)));
            }

            result.warnings.forEach(warning => this.logWarning(warning));

            return result;
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);
            console.error(chalk.red(`[ERROR] ${this.featureName} generation failed for ${aggregateName}: ${errorMessage}`));

            this.handleFeatureError(
                error instanceof Error ? error : new Error(errorMessage),
                'feature generation',
                { aggregate: aggregateName }
            );

            return this.createErrorResult([errorMessage]);
        }
    }

    

    protected hasGenerator(generators: any, generatorName: string): boolean {
        return generators &&
            generators[generatorName] &&
            typeof generators[generatorName].generate === 'function';
    }

    

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
