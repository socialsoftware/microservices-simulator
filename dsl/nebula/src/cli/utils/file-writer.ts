import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import { ErrorHandler, ErrorUtils, ErrorSeverity } from './error-handler.js';
import { InputValidator, ValidationError } from './input-validator.js';

/**
 * Centralized file writing utility to eliminate duplication across feature classes.
 * Provides consistent file operations with proper directory creation and logging.
 * Includes security validation to prevent path traversal attacks.
 */
export class FileWriter {
    // Base directory for security validation (set during initialization)
    private static baseOutputDir: string | null = null;

    /**
     * Set the base output directory for security validation
     * @param baseDir - The base directory that all file operations should be within
     */
    static setBaseOutputDirectory(baseDir: string): void {
        this.baseOutputDir = path.resolve(baseDir);
    }
    /**
     * Validate file path for security before any file operation
     * @param filePath - The file path to validate
     * @returns Validated and normalized file path
     * @throws ValidationError if path is invalid or unsafe
     */
    private static validateFilePath(filePath: string): string {
        // Basic path validation
        const validation = InputValidator.validateFilePath(filePath, this.baseOutputDir || undefined);
        if (!validation.isValid) {
            throw new ValidationError(validation.error || 'Invalid file path', 'filePath', filePath);
        }

        const normalizedPath = validation.sanitized!;

        // Additional security check if base directory is set
        if (this.baseOutputDir) {
            const resolvedPath = path.resolve(normalizedPath);
            const resolvedBase = path.resolve(this.baseOutputDir);

            if (!resolvedPath.startsWith(resolvedBase)) {
                throw new ValidationError(
                    `File path '${filePath}' is outside the allowed output directory '${this.baseOutputDir}'`,
                    'filePath',
                    filePath
                );
            }
        }

        return normalizedPath;
    }

    /**
     * Write a single generated file with consistent logging and directory creation.
     * Includes security validation to prevent path traversal attacks.
     * 
     * @param filePath - The full path where the file should be written
     * @param content - The content to write to the file
     * @param description - Human-readable description for logging (e.g., "entity User", "service UserService")
     */
    static async writeGeneratedFile(filePath: string, content: string, description: string): Promise<void> {
        try {
            // Validate and sanitize the file path for security
            const safePath = this.validateFilePath(filePath);

            // Ensure the directory exists
            await fs.mkdir(path.dirname(safePath), { recursive: true });

            // Write the file
            await fs.writeFile(safePath, content, 'utf-8');

            // Log the successful generation
            console.log(`\t- Generated ${description}`);
        } catch (error) {
            if (error instanceof ValidationError) {
                ErrorHandler.handle(
                    error,
                    ErrorUtils.fileContext('validate file path', filePath, { description }),
                    ErrorSeverity.FATAL
                );
            } else {
                ErrorHandler.handle(
                    error instanceof Error ? error : new Error(String(error)),
                    ErrorUtils.fileContext('write generated file', filePath, { description }),
                    ErrorSeverity.FATAL
                );
            }
        }
    }

    /**
     * Write multiple files in batch with consistent logging.
     * Useful for generators that produce multiple related files.
     * 
     * @param files - Map of file paths to their content
     * @param basePath - Optional base path to prepend to relative file paths
     * @param logPrefix - Optional prefix for log messages (e.g., "saga", "event")
     */
    static async writeMultipleFiles(
        files: Map<string, string>,
        basePath?: string,
        logPrefix?: string
    ): Promise<void> {
        const writePromises: Promise<void>[] = [];

        for (const [relativePath, content] of files.entries()) {
            const fullPath = basePath ? path.join(basePath, relativePath) : relativePath;
            const description = logPrefix ? `${logPrefix} ${path.basename(relativePath, '.java')}` : path.basename(relativePath, '.java');

            writePromises.push(this.writeGeneratedFile(fullPath, content, description));
        }

        // Execute all writes in parallel for better performance
        await Promise.all(writePromises);
    }

    /**
     * Write files from a key-value object (common pattern in generators).
     * 
     * @param filesObject - Object with keys as file identifiers and values as content
     * @param pathBuilder - Function to build file path from key
     * @param descriptionBuilder - Function to build description from key
     */
    static async writeFilesFromObject(
        filesObject: { [key: string]: string },
        pathBuilder: (key: string) => string,
        descriptionBuilder: (key: string) => string
    ): Promise<void> {
        const writePromises: Promise<void>[] = [];

        for (const [key, content] of Object.entries(filesObject)) {
            if (typeof content === 'string') {
                const filePath = pathBuilder(key);
                const description = descriptionBuilder(key);
                writePromises.push(this.writeGeneratedFile(filePath, content, description));
            }
        }

        await Promise.all(writePromises);
    }

    /**
     * Ensure a directory exists (utility method for cases where only directory creation is needed).
     * Includes security validation to prevent path traversal attacks.
     * 
     * @param dirPath - The directory path to create
     */
    static async ensureDirectory(dirPath: string): Promise<void> {
        try {
            // Validate the directory path for security
            const safePath = this.validateFilePath(dirPath);
            await fs.mkdir(safePath, { recursive: true });
        } catch (error) {
            if (error instanceof ValidationError) {
                throw error;
            }
            throw new Error(`Failed to create directory ${dirPath}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }

    /**
     * Write a file only if the content has changed (optimization for incremental builds).
     * 
     * @param filePath - The full path where the file should be written
     * @param content - The content to write to the file
     * @param description - Human-readable description for logging
     */
    static async writeIfChanged(filePath: string, content: string, description: string): Promise<boolean> {
        try {
            // Check if file exists and has the same content
            try {
                const existingContent = await fs.readFile(filePath, 'utf-8');
                if (existingContent === content) {
                    return false; // No change needed
                }
            } catch {
                // File doesn't exist, proceed with write
            }

            await this.writeGeneratedFile(filePath, content, description);
            return true; // File was written
        } catch (error) {
            throw new Error(`Failed to write file ${filePath}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
