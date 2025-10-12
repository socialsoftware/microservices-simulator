/**
 * Standardized Error Handling System
 * 
 * This module provides consistent error handling across the DSL system,
 * replacing scattered error handling patterns with a unified approach.
 */

/**
 * Context information for better error debugging and reporting
 */
export interface ErrorContext {
    operation: string;
    aggregateName?: string;
    entityName?: string;
    fileName?: string;
    generatorType?: string;
    phase?: string;
    additionalInfo?: Record<string, any>;
}

/**
 * Enhanced error class with context information for generation errors
 */
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

        // Maintain proper stack trace
        if (Error.captureStackTrace) {
            Error.captureStackTrace(this, GenerationError);
        }
    }

    /**
     * Get a formatted error message with context
     */
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

    /**
     * Get detailed error information for debugging
     */
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

/**
 * Error severity levels for different types of issues
 */
export enum ErrorSeverity {
    WARNING = 'warning',
    ERROR = 'error',
    FATAL = 'fatal'
}

/**
 * Centralized error handler with consistent logging and error management
 */
export class ErrorHandler {
    private static errorCount = 0;
    private static warningCount = 0;

    /**
     * Handle an error with consistent logging and optional re-throwing
     */
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

        // Log based on severity
        switch (severity) {
            case ErrorSeverity.WARNING:
                this.warningCount++;
                console.warn(`⚠️  ${generationError.getFormattedMessage()}`);
                break;
            case ErrorSeverity.ERROR:
                this.errorCount++;
                console.error(`❌ ${generationError.getFormattedMessage()}`);
                break;
            case ErrorSeverity.FATAL:
                this.errorCount++;
                console.error(`💥 FATAL: ${generationError.getDetailedInfo()}`);
                break;
        }

        // Re-throw if requested
        if (shouldThrow && severity !== ErrorSeverity.WARNING) {
            throw generationError;
        }
    }

    /**
     * Wrap an async function with consistent error handling
     */
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

    /**
     * Wrap a synchronous function with consistent error handling
     */
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

    /**
     * Create a context-aware error thrower for specific operations
     */
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

    /**
     * Get error statistics
     */
    static getStats(): { errors: number; warnings: number } {
        return {
            errors: this.errorCount,
            warnings: this.warningCount
        };
    }

    /**
     * Reset error counters
     */
    static resetStats(): void {
        this.errorCount = 0;
        this.warningCount = 0;
    }

    /**
     * Check if there were any errors during generation
     */
    static hasErrors(): boolean {
        return this.errorCount > 0;
    }

    /**
     * Print final error summary
     */
    static printSummary(): void {
        const stats = this.getStats();
        if (stats.errors > 0 || stats.warnings > 0) {
            console.log('\n📊 Generation Summary:');
            if (stats.errors > 0) {
                console.log(`   ❌ Errors: ${stats.errors}`);
            }
            if (stats.warnings > 0) {
                console.log(`   ⚠️  Warnings: ${stats.warnings}`);
            }
        }
    }
}

/**
 * Utility functions for common error scenarios
 */
export class ErrorUtils {
    /**
     * Create error context for file operations
     */
    static fileContext(operation: string, fileName: string, additionalInfo?: Record<string, any>): ErrorContext {
        return {
            operation,
            fileName,
            generatorType: 'file-operation',
            additionalInfo
        };
    }

    /**
     * Create error context for aggregate operations
     */
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

    /**
     * Create error context for entity operations
     */
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

    /**
     * Create error context for template operations
     */
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

    /**
     * Extract meaningful error message from any error type
     */
    static extractMessage(error: unknown): string {
        if (error instanceof Error) {
            return error.message;
        }
        if (typeof error === 'string') {
            return error;
        }
        return String(error);
    }

    /**
     * Check if error is a specific type
     */
    static isGenerationError(error: unknown): error is GenerationError {
        return error instanceof GenerationError;
    }

    /**
     * Safe error logging that handles any error type
     */
    static safeLog(error: unknown, prefix: string = 'Error'): void {
        const message = this.extractMessage(error);
        console.error(`${prefix}: ${message}`);

        if (error instanceof Error && error.stack) {
            console.error('Stack trace:', error.stack);
        }
    }
}
