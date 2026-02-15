import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import { ErrorHandler, ErrorUtils, ErrorSeverity } from './error-handler.js';
import { InputValidator, ValidationError } from './input-validator.js';



export class FileWriter {
    
    private static baseOutputDir: string | null = null;

    

    static setBaseOutputDirectory(baseDir: string): void {
        this.baseOutputDir = path.resolve(baseDir);
    }
    

    private static validateFilePath(filePath: string): string {
        
        const validation = InputValidator.validateFilePath(filePath, this.baseOutputDir || undefined);
        if (!validation.isValid) {
            throw new ValidationError(validation.error || 'Invalid file path', 'filePath', filePath);
        }

        const normalizedPath = validation.sanitized!;

        
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

    

    static async writeGeneratedFile(filePath: string, content: string, description: string): Promise<void> {
        try {
            
            const safePath = this.validateFilePath(filePath);

            
            await fs.mkdir(path.dirname(safePath), { recursive: true });

            
            await fs.writeFile(safePath, content, 'utf-8');

            
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

        
        await Promise.all(writePromises);
    }

    

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

    

    static async ensureDirectory(dirPath: string): Promise<void> {
        try {
            
            const safePath = this.validateFilePath(dirPath);
            await fs.mkdir(safePath, { recursive: true });
        } catch (error) {
            if (error instanceof ValidationError) {
                throw error;
            }
            throw new Error(`Failed to create directory ${dirPath}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }

    

    static async writeIfChanged(filePath: string, content: string, description: string): Promise<boolean> {
        try {
            
            try {
                const existingContent = await fs.readFile(filePath, 'utf-8');
                if (existingContent === content) {
                    return false; 
                }
            } catch {
                
            }

            await this.writeGeneratedFile(filePath, content, description);
            return true; 
        } catch (error) {
            throw new Error(`Failed to write file ${filePath}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
