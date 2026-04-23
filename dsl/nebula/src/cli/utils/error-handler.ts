


import chalk from 'chalk';

export interface ErrorContext {
    operation: string;
    aggregateName?: string;
    entityName?: string;
    fileName?: string;
    generatorType?: string;
    phase?: string;
    additionalInfo?: Record<string, any>;
}



export class GenerationError extends Error {
    public readonly context: ErrorContext;
    public readonly timestamp: Date;
    public readonly originalError?: Error;

    constructor(message: string, context: ErrorContext, originalError?: Error) {
        super(message);
        this.name = 'GenerationError';
        this.context = context;
        this.timestamp = new Date();
        this.originalError = originalError;

        
        if (Error.captureStackTrace) {
            Error.captureStackTrace(this, GenerationError);
        }
    }

    

    getFormattedMessage(): string {
        const parts = [this.message];

        if (this.context.aggregateName) {
            parts.push(`[Aggregate: ${this.context.aggregateName}]`);
        }

        if (this.context.entityName) {
            parts.push(`[Entity: ${this.context.entityName}]`);
        }

        if (this.context.fileName) {
            parts.push(`[File: ${this.context.fileName}]`);
        }

        if (this.context.generatorType) {
            parts.push(`[Generator: ${this.context.generatorType}]`);
        }

        return parts.join(' ');
    }

    

    getDetailedInfo(): string {
        const info = [
            `Error: ${this.getFormattedMessage()}`,
            `Operation: ${this.context.operation}`,
            `Timestamp: ${this.timestamp.toISOString()}`
        ];

        if (this.context.phase) {
            info.push(`Phase: ${this.context.phase}`);
        }

        if (this.context.additionalInfo) {
            info.push(`Additional Info: ${JSON.stringify(this.context.additionalInfo, null, 2)}`);
        }

        if (this.originalError) {
            info.push(`Original Error: ${this.originalError.message}`);
            if (this.originalError.stack) {
                info.push(`Original Stack: ${this.originalError.stack}`);
            }
        }

        return info.join('\n');
    }
}



export enum ErrorSeverity {
    WARNING = 'warning',
    ERROR = 'error',
    FATAL = 'fatal'
}



export class ErrorHandler {
    private static errorCount = 0;
    private static warningCount = 0;

    

    private static getIcon(severity: ErrorSeverity): string {
        return {
            [ErrorSeverity.WARNING]: chalk.yellow('[WARN]'),
            [ErrorSeverity.ERROR]: chalk.red('[ERROR]'),
            [ErrorSeverity.FATAL]: chalk.red('[FATAL]')
        }[severity];
    }

    

    static handle(
        error: Error | GenerationError,
        context: ErrorContext,
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        shouldThrow: boolean = true
    ): void {
        let generationError: GenerationError;

        if (error instanceof GenerationError) {
            generationError = error;
        } else {
            generationError = new GenerationError(error.message, context, error);
        }

        
        const icon = this.getIcon(severity);
        switch (severity) {
            case ErrorSeverity.WARNING:
                this.warningCount++;
                console.warn(`${icon} ${generationError.getFormattedMessage()}`);
                break;
            case ErrorSeverity.ERROR:
                this.errorCount++;
                console.error(`${icon} ${generationError.getFormattedMessage()}`);
                break;
            case ErrorSeverity.FATAL:
                this.errorCount++;
                console.error(`${icon} FATAL: ${generationError.getDetailedInfo()}`);
                break;
        }

        
        if (shouldThrow && severity !== ErrorSeverity.WARNING) {
            throw generationError;
        }
    }

    

    static async wrapAsync<T>(
        operation: () => Promise<T>,
        context: ErrorContext,
        severity: ErrorSeverity = ErrorSeverity.ERROR
    ): Promise<T | null> {
        try {
            return await operation();
        } catch (error) {
            this.handle(
                error instanceof Error ? error : new Error(String(error)),
                context,
                severity,
                severity === ErrorSeverity.FATAL
            );
            return null;
        }
    }

    

    static wrap<T>(
        operation: () => T,
        context: ErrorContext,
        severity: ErrorSeverity = ErrorSeverity.ERROR
    ): T | null {
        try {
            return operation();
        } catch (error) {
            this.handle(
                error instanceof Error ? error : new Error(String(error)),
                context,
                severity,
                severity === ErrorSeverity.FATAL
            );
            return null;
        }
    }

    

    static createErrorThrower(baseContext: Partial<ErrorContext>) {
        return (message: string, additionalContext?: Partial<ErrorContext>, originalError?: Error) => {
            const fullContext: ErrorContext = {
                operation: 'unknown',
                ...baseContext,
                ...additionalContext
            };
            throw new GenerationError(message, fullContext, originalError);
        };
    }

    

    static getStats(): { errors: number; warnings: number } {
        return {
            errors: this.errorCount,
            warnings: this.warningCount
        };
    }

    

    static resetStats(): void {
        this.errorCount = 0;
        this.warningCount = 0;
    }

    

    static hasErrors(): boolean {
        return this.errorCount > 0;
    }

    

    static printSummary(): void {
        const stats = this.getStats();
        if (stats.errors > 0 || stats.warnings > 0) {
            console.log('\n' + chalk.bold('Generation Summary:'));
            if (stats.errors > 0) {
                console.log(chalk.red(`  Errors: ${stats.errors}`));
            }
            if (stats.warnings > 0) {
                console.log(chalk.yellow(`  Warnings: ${stats.warnings}`));
            }
        }
    }
}



export class ErrorUtils {
    

    static fileContext(operation: string, fileName: string, additionalInfo?: Record<string, any>): ErrorContext {
        return {
            operation,
            fileName,
            generatorType: 'file-operation',
            additionalInfo
        };
    }

    

    static aggregateContext(
        operation: string,
        aggregateName: string,
        generatorType?: string,
        additionalInfo?: Record<string, any>
    ): ErrorContext {
        return {
            operation,
            aggregateName,
            generatorType,
            additionalInfo
        };
    }

    

    static entityContext(
        operation: string,
        aggregateName: string,
        entityName: string,
        generatorType?: string,
        additionalInfo?: Record<string, any>
    ): ErrorContext {
        return {
            operation,
            aggregateName,
            entityName,
            generatorType,
            additionalInfo
        };
    }

    

    static templateContext(
        operation: string,
        templateName: string,
        additionalInfo?: Record<string, any>
    ): ErrorContext {
        return {
            operation,
            fileName: templateName,
            generatorType: 'template',
            additionalInfo
        };
    }

    

    static extractMessage(error: unknown): string {
        if (error instanceof Error) {
            return error.message;
        }
        if (typeof error === 'string') {
            return error;
        }
        return String(error);
    }

    

    static isGenerationError(error: unknown): error is GenerationError {
        return error instanceof GenerationError;
    }

    

    static safeLog(error: unknown, prefix: string = 'Error'): void {
        const message = this.extractMessage(error);
        console.error(`${prefix}: ${message}`);

        if (error instanceof Error && error.stack) {
            console.error('Stack trace:', error.stack);
        }
    }
}
