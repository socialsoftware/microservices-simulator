const originalStderrWrite = process.stderr.write.bind(process.stderr);
const originalStdoutWrite = process.stdout.write.bind(process.stdout);
const originalConsoleError = console.error.bind(console);
const originalConsoleWarn = console.warn.bind(console);
const originalConsoleLog = console.log.bind(console);

const shouldSuppress = (message: string): boolean => {
    const msg = message || '';
    return msg.includes('Ambiguous Alternatives Detected') ||
        msg.includes('chevrotain.io/docs/guide/resolving_grammar_errors') ||
        msg.includes('may appears as a prefix path') ||
        msg.includes('See: https://chevrotain.io') ||
        msg.includes('For Further details');
};

// Intercept stderr
process.stderr.write = ((chunk: any, encoding?: any, callback?: any) => {
    const message = chunk?.toString() || '';
    if (shouldSuppress(message)) {
        if (typeof callback === 'function') {
            callback();
        }
        return true;
    }
    return originalStderrWrite(chunk, encoding, callback);
}) as any;

process.stdout.write = ((chunk: any, encoding?: any, callback?: any) => {
    const message = chunk?.toString() || '';
    if (shouldSuppress(message)) {
        if (typeof callback === 'function') {
            callback();
        }
        return true;
    }
    return originalStdoutWrite(chunk, encoding, callback);
}) as any;

console.error = ((...args: any[]) => {
    const message = args.map(a => String(a)).join(' ');
    if (!shouldSuppress(message)) {
        originalConsoleError(...args);
    }
}) as any;

console.warn = ((...args: any[]) => {
    const message = args.map(a => String(a)).join(' ');
    if (!shouldSuppress(message)) {
        originalConsoleWarn(...args);
    }
}) as any;

console.log = ((...args: any[]) => {
    const message = args.map(a => String(a)).join(' ');
    if (!shouldSuppress(message)) {
        originalConsoleLog(...args);
    }
}) as any;

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
                name: path.basename(path.resolve(abstractionsPath)),
                validate: true
            });
        });

    program.parse(process.argv);
}
