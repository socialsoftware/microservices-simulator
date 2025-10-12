import { Aggregate, Model } from "../../../../../language/generated/ast.js";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { fileURLToPath } from 'node:url';
const __dirname = path.dirname(fileURLToPath(import.meta.url));

export interface ExceptionGenerationOptions {
    projectName: string;
    packageName: string;
    architecture?: string;
    features?: string[];
}

export class ExceptionGenerator {
    async generate(aggregate: Aggregate, outputPath: string, options: ExceptionGenerationOptions, allModels: Model[]): Promise<void> {
        const projectName = options.projectName;
        const packageName = options.packageName;

        const errorMessages = this.extractErrorMessagesFromDSL(allModels);

        const exceptionClass = await this.generateExceptionClass(projectName, packageName);
        const capitalizedProjectName = projectName.charAt(0).toUpperCase() + projectName.slice(1);
        const exceptionPath = path.join(outputPath, 'src', 'main', 'java', ...packageName.split('.'), 'microservices', 'exception', `${capitalizedProjectName}Exception.java`);
        await this.ensureDirectoryExists(path.dirname(exceptionPath));
        await fs.writeFile(exceptionPath, exceptionClass);

        const errorMessageClass = await this.generateErrorMessageClass(projectName, packageName, errorMessages);
        const errorMessagePath = path.join(outputPath, 'src', 'main', 'java', ...packageName.split('.'), 'microservices', 'exception', `${capitalizedProjectName}ErrorMessage.java`);
        await fs.writeFile(errorMessagePath, errorMessageClass);

        const controllerAdviceClass = await this.generateControllerAdviceClass(projectName, packageName);
        const advicePath = path.join(outputPath, 'src', 'main', 'java', ...packageName.split('.'), 'microservices', 'exception', `${capitalizedProjectName}ExceptionHandler.java`);
        await fs.writeFile(advicePath, controllerAdviceClass);

        console.log(`        - Generated exception ${capitalizedProjectName}Exception`);
        console.log(`        - Generated error messages ${capitalizedProjectName}ErrorMessage`);
        console.log(`        - Generated exception handler ${capitalizedProjectName}ExceptionHandler`);
    }

    private extractErrorMessagesFromDSL(allModels: Model[]): Array<{ name: string, message: string }> {
        const errorMessages: Array<{ name: string, message: string }> = [];

        for (const model of allModels) {
            if (model.exceptions && model.exceptions.messages) {
                for (const message of model.exceptions.messages) {
                    errorMessages.push({
                        name: message.name,
                        message: message.message
                    });
                }
            }
        }

        return errorMessages;
    }

    private async generateExceptionClass(projectName: string, packageName: string): Promise<string> {
        const capitalizedProjectName = projectName.charAt(0).toUpperCase() + projectName.slice(1);
        return `package ${packageName}.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ${capitalizedProjectName}Exception extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(${capitalizedProjectName}Exception.class);
    private final String ${projectName.toLowerCase()}ErrorMessage;

    public ${capitalizedProjectName}Exception(String ${projectName.toLowerCase()}ErrorMessage) {
        super(${projectName.toLowerCase()}ErrorMessage);
        logger.info(${projectName.toLowerCase()}ErrorMessage);
        this.${projectName.toLowerCase()}ErrorMessage = ${projectName.toLowerCase()}ErrorMessage;
    }

    public ${capitalizedProjectName}Exception(String ${projectName.toLowerCase()}ErrorMessage, String value) {
        super(String.format(${projectName.toLowerCase()}ErrorMessage, value));
        logger.info(String.format(${projectName.toLowerCase()}ErrorMessage, value));
        this.${projectName.toLowerCase()}ErrorMessage = ${projectName.toLowerCase()}ErrorMessage;
    }

    public ${capitalizedProjectName}Exception(String ${projectName.toLowerCase()}ErrorMessage, String value1, String value2) {
        super(String.format(${projectName.toLowerCase()}ErrorMessage, value1, value2));
        logger.info(String.format(${projectName.toLowerCase()}ErrorMessage, value1, value2));
        this.${projectName.toLowerCase()}ErrorMessage = ${projectName.toLowerCase()}ErrorMessage;
    }

    public ${capitalizedProjectName}Exception(String ${projectName.toLowerCase()}ErrorMessage, int value) {
        super(String.format(${projectName.toLowerCase()}ErrorMessage, value));
        logger.info(String.format(${projectName.toLowerCase()}ErrorMessage, value));
        this.${projectName.toLowerCase()}ErrorMessage = ${projectName.toLowerCase()}ErrorMessage;
    }

    public ${capitalizedProjectName}Exception(String ${projectName.toLowerCase()}ErrorMessage, int value1, int value2) {
        super(String.format(${projectName.toLowerCase()}ErrorMessage, value1, value2));
        logger.info(String.format(${projectName.toLowerCase()}ErrorMessage, value1, value2));
        this.${projectName.toLowerCase()}ErrorMessage = ${projectName.toLowerCase()}ErrorMessage;
    }

    public ${capitalizedProjectName}Exception(String ${projectName.toLowerCase()}ErrorMessage, String value1, int value2) {
        super(String.format(${projectName.toLowerCase()}ErrorMessage, value1, value2));
        logger.info(String.format(${projectName.toLowerCase()}ErrorMessage, value1, value2));
        this.${projectName.toLowerCase()}ErrorMessage = ${projectName.toLowerCase()}ErrorMessage;
    }

    public String getErrorMessage() {
        return this.${projectName.toLowerCase()}ErrorMessage;
    }
}`;
    }

    private async generateErrorMessageClass(projectName: string, packageName: string, errorMessages: Array<{ name: string, message: string }>): Promise<string> {
        const capitalizedProjectName = projectName.charAt(0).toUpperCase() + projectName.slice(1);

        let constants = '';
        for (const errorMessage of errorMessages) {
            const constantName = errorMessage.name.toUpperCase();
            const messageValue = (errorMessage.message || '').replace(/"/g, '\\"');
            constants += `    public static final String ${constantName} = "${messageValue}";\n\n`;
        }

        const templatePath = path.join(__dirname, '../../../../templates', 'exceptions', 'error-messages.hbs');
        try {
            const tpl = await fs.readFile(templatePath, 'utf-8');
            return tpl
                .replace(/\{\{packageName\}\}/g, packageName)
                .replace(/\{\{ProjectName\}\}/g, capitalizedProjectName)
                .replace(/\{\{constants\}\}/g, constants);
        } catch {
            return `package ${packageName}.microservices.exception;\n\npublic final class ${capitalizedProjectName}ErrorMessage {\n    private ${capitalizedProjectName}ErrorMessage() {}\n\n${constants}}`;
        }
    }

    private async ensureDirectoryExists(dirPath: string): Promise<void> {
        try {
            await fs.mkdir(dirPath, { recursive: true });
        } catch (error) {
        }
    }

    private async generateControllerAdviceClass(projectName: string, packageName: string): Promise<string> {
        const capitalizedProjectName = projectName.charAt(0).toUpperCase() + projectName.slice(1);
        return `package ${packageName}.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ControllerAdvice
public class ${capitalizedProjectName}ExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(${capitalizedProjectName}ExceptionHandler.class);

    @ExceptionHandler(${capitalizedProjectName}Exception.class)
    public ResponseEntity<String> handle${capitalizedProjectName}Exception(${capitalizedProjectName}Exception ex) {
        logger.warn("Handled ${capitalizedProjectName}Exception: {}", ex.getErrorMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(SimulatorException.class)
    public ResponseEntity<String> handleSimulatorException(SimulatorException ex) {
        logger.warn("Handled SimulatorException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        logger.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
    }
}`;
    }
}
