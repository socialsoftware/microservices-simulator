import type { AstNode, LangiumCoreServices, LangiumDocument } from 'langium';
import chalk from 'chalk';
import * as path from 'node:path';
import * as fs from 'node:fs';
import { URI } from 'langium';
import { InputValidator } from './input-validator.js';

export async function extractDocument(fileName: string, services: LangiumCoreServices): Promise<LangiumDocument> {
    // Validate file path for security
    try {
        const pathValidation = InputValidator.validateFilePath(fileName);
        if (!pathValidation.isValid) {
            console.error(chalk.red(`Invalid file path: ${pathValidation.error}`));
            process.exit(1);
        }
        fileName = pathValidation.sanitized!;
    } catch (error) {
        console.error(chalk.red(`File path validation failed: ${error instanceof Error ? error.message : String(error)}`));
        process.exit(1);
    }

    // Validate file extension
    const extensions = services.LanguageMetaData.fileExtensions;
    const extensionValidation = InputValidator.validateFileExtension(fileName, [...extensions]);
    if (!extensionValidation.isValid) {
        console.error(chalk.yellow(`${extensionValidation.error}. Expected one of: ${extensions.join(', ')}`));
        process.exit(1);
    }

    // Check if file exists
    if (!fs.existsSync(fileName)) {
        console.error(chalk.red(`File ${fileName} does not exist.`));
        process.exit(1);
    }

    // Check file size (prevent processing extremely large files)
    const stats = fs.statSync(fileName);
    const maxFileSize = 10 * 1024 * 1024; // 10MB limit
    if (stats.size > maxFileSize) {
        console.error(chalk.red(`File ${fileName} is too large (${Math.round(stats.size / 1024 / 1024)}MB). Maximum allowed size is ${maxFileSize / 1024 / 1024}MB.`));
        process.exit(1);
    }

    // Check if file is readable
    try {
        fs.accessSync(fileName, fs.constants.R_OK);
    } catch (error) {
        console.error(chalk.red(`File ${fileName} is not readable.`));
        process.exit(1);
    }

    const document = await services.shared.workspace.LangiumDocuments.getOrCreateDocument(URI.file(path.resolve(fileName)));
    // Don't rebuild - documents are already built in main.ts
    // await services.shared.workspace.DocumentBuilder.build(Array.from(services.shared.workspace.LangiumDocuments.all), { validation: true });

    const validationErrors = (document.diagnostics ?? []).filter((e: any) => e.severity === 1);
    if (validationErrors.length > 0) {
        const filePath = document.uri.fsPath || document.uri.path;
        const relativePath = path.relative(process.cwd(), filePath);
        console.error(chalk.red('There are validation errors:'));
        for (const validationError of validationErrors) {
            const lineNum = validationError.range.start.line + 1;
            const colNum = validationError.range.start.character + 1;
            const errorText = document.textDocument.getText(validationError.range);
            console.error(chalk.red(
                `${relativePath}:${lineNum}:${colNum} - ${validationError.message}${errorText ? ` [${errorText}]` : ''}`
            ));
        }
        process.exit(1);
    }

    return document;
}

export async function extractAstNode<T extends AstNode>(fileName: string, services: LangiumCoreServices): Promise<T> {
    return (await extractDocument(fileName, services)).parseResult?.value as T;
}

interface FilePathData {
    destination: string,
    name: string
}

export function extractDestinationAndName(filePath: string, destination: string | undefined): FilePathData {
    filePath = path.basename(filePath, path.extname(filePath)).replace(/[.-]/g, '');
    return {
        destination: destination ?? path.join(path.dirname(filePath), 'generated'),
        name: path.basename(filePath)
    };
}
