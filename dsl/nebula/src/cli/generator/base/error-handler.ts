export enum ErrorType {
    VALIDATION = 'validation',
    TEMPLATE = 'template',
    FILE_SYSTEM = 'file_system',
    COMPILATION = 'compilation',
    CONFIGURATION = 'configuration',
    ARCHITECTURE = 'architecture'
}

export enum ErrorSeverity {
    ERROR = 'error',
    WARNING = 'warning',
    INFO = 'info'
}

export interface GenerationError {
    type: ErrorType;
    severity: ErrorSeverity;
    code: string;
    message: string;
    file?: string;
    line?: number;
    column?: number;
    context?: any;
    timestamp: Date;
}

export class ErrorHandler {
    private errors: GenerationError[] = [];
    private warnings: GenerationError[] = [];
    private infos: GenerationError[] = [];

    addError(
        type: ErrorType,
        code: string,
        message: string,
        file?: string,
        line?: number,
        column?: number,
        context?: any
    ): void {
        const error: GenerationError = {
            type,
            severity: ErrorSeverity.ERROR,
            code,
            message,
            file,
            line,
            column,
            context,
            timestamp: new Date()
        };

        this.errors.push(error);
        this.logError(error);
    }

    addWarning(
        type: ErrorType,
        code: string,
        message: string,
        file?: string,
        line?: number,
        column?: number,
        context?: any
    ): void {
        const warning: GenerationError = {
            type,
            severity: ErrorSeverity.WARNING,
            code,
            message,
            file,
            line,
            column,
            context,
            timestamp: new Date()
        };

        this.warnings.push(warning);
        this.logWarning(warning);
    }

    addInfo(
        type: ErrorType,
        code: string,
        message: string,
        file?: string,
        context?: any
    ): void {
        const info: GenerationError = {
            type,
            severity: ErrorSeverity.INFO,
            code,
            message,
            file,
            context,
            timestamp: new Date()
        };

        this.infos.push(info);
        this.logInfo(info);
    }

    getErrors(): GenerationError[] {
        return [...this.errors];
    }

    getWarnings(): GenerationError[] {
        return [...this.warnings];
    }

    getInfos(): GenerationError[] {
        return [...this.infos];
    }

    getAllIssues(): GenerationError[] {
        return [...this.errors, ...this.warnings, ...this.infos];
    }

    hasErrors(): boolean {
        return this.errors.length > 0;
    }

    hasWarnings(): boolean {
        return this.warnings.length > 0;
    }

    getErrorCount(): number {
        return this.errors.length;
    }

    getWarningCount(): number {
        return this.warnings.length;
    }

    clear(): void {
        this.errors = [];
        this.warnings = [];
        this.infos = [];
    }

    generateSummary(): string {
        const lines: string[] = [];

        lines.push('=== Generation Summary ===');
        lines.push(`Errors: ${this.errors.length}`);
        lines.push(`Warnings: ${this.warnings.length}`);
        lines.push(`Info: ${this.infos.length}`);
        lines.push('');

        if (this.errors.length > 0) {
            lines.push('ERRORS:');
            this.errors.forEach(error => {
                lines.push(this.formatError(error));
            });
            lines.push('');
        }

        if (this.warnings.length > 0) {
            lines.push('WARNINGS:');
            this.warnings.forEach(warning => {
                lines.push(this.formatError(warning));
            });
            lines.push('');
        }

        return lines.join('\n');
    }

    private formatError(error: GenerationError): string {
        let formatted = `[${error.type.toUpperCase()}:${error.code}] ${error.message}`;

        if (error.file) {
            formatted += ` (${error.file}`;
            if (error.line !== undefined) {
                formatted += `:${error.line}`;
                if (error.column !== undefined) {
                    formatted += `:${error.column}`;
                }
            }
            formatted += ')';
        }

        return formatted;
    }

    private logError(error: GenerationError): void {
        console.error(`❌ ${this.formatError(error)}`);
    }

    private logWarning(warning: GenerationError): void {
        console.warn(`⚠️  ${this.formatError(warning)}`);
    }

    private logInfo(info: GenerationError): void {
        console.info(`ℹ️  ${this.formatError(info)}`);
    }

    static wrap<T>(
        errorHandler: ErrorHandler,
        fn: () => T,
        errorType: ErrorType = ErrorType.COMPILATION,
        errorCode: string = 'UNKNOWN_ERROR'
    ): T | null {
        try {
            return fn();
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            errorHandler.addError(errorType, errorCode, message);
            return null;
        }
    }

    static async wrapAsync<T>(
        errorHandler: ErrorHandler,
        fn: () => Promise<T>,
        errorType: ErrorType = ErrorType.COMPILATION,
        errorCode: string = 'UNKNOWN_ERROR'
    ): Promise<T | null> {
        try {
            return await fn();
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            errorHandler.addError(errorType, errorCode, message);
            return null;
        }
    }
}

let globalErrorHandler: ErrorHandler | null = null;

export function getGlobalErrorHandler(): ErrorHandler {
    if (!globalErrorHandler) {
        globalErrorHandler = new ErrorHandler();
    }
    return globalErrorHandler;
}

export function initializeErrorHandler(): ErrorHandler {
    globalErrorHandler = new ErrorHandler();
    return globalErrorHandler;
}
