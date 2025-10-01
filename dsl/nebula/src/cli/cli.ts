/**
 * CLI entry point for the Nebula DSL code generator
 */

import { Command } from "commander";
import { NebulaLanguageMetaData } from "../language/generated/module.js";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { CodeGenerator } from "./core/code-generator.js";
import { DEFAULT_OUTPUT_DIR } from "./core/types.js";

const __dirname = path.dirname(new URL(import.meta.url).pathname);
const packagePath = path.resolve(__dirname, "..", "..", "package.json");

export default async function (): Promise<void> {
    const program = new Command();

    const packageContent = await fs.readFile(packagePath, "utf-8");
    program.version(JSON.parse(packageContent).version);

    const fileExtensions = NebulaLanguageMetaData.fileExtensions.join(", ");

    program
        .command("generate")
        .argument(
            "<path>",
            `source file or directory (possible file extensions: ${fileExtensions})`
        )
        .option("-d, --destination <dir>", "destination directory for generating code", DEFAULT_OUTPUT_DIR)
        .option("-n, --name <name>", "name of the project")
        .option("-a, --architecture <arch>", "architecture pattern (default, causal-saga, external-dto-removal)", "default")
        .option("-f, --features <features>", "comma-separated list of features (events,validation,webapi,coordination,saga)", "events,validation,webapi,coordination,saga")
        .option("--validate", "validate DSL files before generation")
        .description('generates Java microservices code from Nebula DSL files')
        .action(async (inputPath: string, options: any) => {
            const features = options.features
                ? options.features.split(',').map((f: string) => f.trim())
                : ['events', 'validation', 'webapi', 'coordination', 'saga'];

            await CodeGenerator.generateCode(inputPath, {
                destination: options.destination,
                name: options.name,
                architecture: options.architecture,
                features,
                validate: options.validate
            });
        });

    program.parse(process.argv);
}
