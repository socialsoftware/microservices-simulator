import { Command } from "commander";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { CodeGenerator } from "./engine/code-generator.js";
import { DEFAULT_OUTPUT_DIR } from "./engine/types.js";
import { ErrorHandler, ErrorUtils } from "./utils/error-handler.js";
import { InputValidator } from "./utils/input-validator.js";
import chalk from "chalk";

const __dirname = path.dirname(new URL(import.meta.url).pathname);
const packagePath = path.resolve(__dirname, "..", "..", "package.json");

export default async function cli(): Promise<void> {
    const program = new Command();

    const packageContent = await fs.readFile(packagePath, "utf-8");
    program.version(JSON.parse(packageContent).version);

    program
        .command("generate")
        .argument(
            "<abstractions-path>",
            "path to the abstractions folder containing Nebula DSL files"
        )
        .option("-o, --output <dir>", "output directory for generated code", DEFAULT_OUTPUT_DIR)
        .option("-d, --debug", "enable debug mode with detailed error output", false)
        .option("-v, --verbose", "enable verbose logging", false)
        .option("--no-validate", "skip validation during generation")
        .description('generates Java microservices code from Nebula DSL abstractions')
        .action(async (abstractionsPath: string, options: any) => {
            
            ErrorHandler.resetStats();

            try {
                
                await validateAbstractionsPath(abstractionsPath, options.debug);

                
                if (options.output) {
                    validateOutputDirectory(options.output, options.debug);
                }

                if (options.verbose) {
                    console.log(chalk.blue('\n[CONFIG] Generation Configuration:'));
                    console.log(chalk.gray(`   Input Path: ${path.resolve(abstractionsPath)}`));
                    console.log(chalk.gray(`   Output Directory: ${options.output || DEFAULT_OUTPUT_DIR}`));
                    console.log(chalk.gray(`   Validation: ${options.validate ? 'enabled' : 'disabled'}`));
                    console.log(chalk.gray(`   Debug Mode: ${options.debug ? 'enabled' : 'disabled'}\n`));
                }

                
                await CodeGenerator.generateCode(abstractionsPath, {
                    destination: options.output,
                    name: path.basename(path.resolve(abstractionsPath)),
                    validate: options.validate
                });

                
                console.log(chalk.green('\n✓ Code generation completed successfully!'));

                
                const stats = ErrorHandler.getStats();
                if (stats.warnings > 0) {
                    console.log(chalk.yellow(`   ⚠ ${stats.warnings} warning(s) encountered`));
                }

            } catch (error) {
                
                handleCliError(error, options.debug);
                process.exit(1);
            }
        });

    program.parse(process.argv);
}



async function validateAbstractionsPath(abstractionsPath: string, debug: boolean): Promise<void> {
    
    const pathValidation = InputValidator.validateFilePath(abstractionsPath);
    if (!pathValidation.isValid) {
        throw new Error(`Invalid abstractions path: ${pathValidation.error}`);
    }

    
    const resolvedPath = path.resolve(abstractionsPath);

    
    try {
        const stats = await fs.stat(resolvedPath);

        if (!stats.isDirectory()) {
            throw new Error(`Path '${abstractionsPath}' is not a directory`);
        }

        
        await fs.access(resolvedPath, fs.constants.R_OK);

    } catch (error) {
        if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
            throw new Error(`Abstractions path does not exist: ${abstractionsPath}`);
        } else if ((error as NodeJS.ErrnoException).code === 'EACCES') {
            throw new Error(`Permission denied: Cannot read from ${abstractionsPath}`);
        }
        throw error;
    }

    if (debug) {
        console.log(chalk.gray(`✓ Validated abstractions path: ${resolvedPath}`));
    }
}



function validateOutputDirectory(outputDir: string, debug: boolean): void {
    const pathValidation = InputValidator.validateFilePath(outputDir);
    if (!pathValidation.isValid) {
        throw new Error(`Invalid output directory: ${pathValidation.error}`);
    }

    if (debug) {
        console.log(chalk.gray(`✓ Validated output directory: ${path.resolve(outputDir)}`));
    }
}



function handleCliError(error: unknown, debug: boolean): void {
    console.log(''); 

    if (debug) {
        
        console.error(chalk.red('✗ Generation failed with error:\n'));
        ErrorUtils.safeLog(error, chalk.red('Error'));

        
        ErrorHandler.printSummary();

    } else {
        
        const message = ErrorUtils.extractMessage(error);
        console.error(chalk.red(`✗ Generation failed: ${message}`));

        
        console.log(chalk.gray('\n[TIP] Run with --debug flag for detailed error information'));
    }

    console.log(''); 
}
