/**
 * CLI entry point for the Nebula DSL code generator
 */

import { Command } from "commander";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { CodeGenerator } from "./engine/code-generator.js";
import { DEFAULT_OUTPUT_DIR } from "./engine/types.js";

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
        .description('generates Java microservices code from Nebula DSL abstractions')
        .action(async (abstractionsPath: string, options: any) => {
            await CodeGenerator.generateCode(abstractionsPath, {
                destination: options.output,
                name: path.basename(path.resolve(abstractionsPath, "..")), // Use parent folder name as project name
                validate: true
            });
        });

    program.parse(process.argv);
}
