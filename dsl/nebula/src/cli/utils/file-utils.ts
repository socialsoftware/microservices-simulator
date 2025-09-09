import * as path from 'node:path';
import * as fs from 'node:fs/promises';
import { NebulaLanguageMetaData } from "../../language/generated/module.js";

export async function collectNebulaFiles(inputPath: string, recursive = false): Promise<string[]> {
    const resolvedPath = path.resolve(process.cwd(), inputPath);
    const stats = await fs.stat(resolvedPath);

    const nebulaExtensions = NebulaLanguageMetaData.fileExtensions;

    const matchesExtension = (file: string) =>
        nebulaExtensions.some((ext: any) => file.endsWith(ext));

    const results: string[] = [];

    if (stats.isDirectory()) {
        const traverse = async (dir: string): Promise<void> => {
            const entries = await fs.readdir(dir, { withFileTypes: true });
            for (const entry of entries) {
                const entryPath = path.join(dir, entry.name);
                if (entry.isDirectory()) {
                    if (recursive) {
                        await traverse(entryPath);
                    }
                    continue;
                }
                if (matchesExtension(entry.name)) {
                    results.push(entryPath);
                }
            }
        };
        await traverse(resolvedPath);
    } else if (stats.isFile()) {
        if (matchesExtension(resolvedPath)) {
            results.push(resolvedPath);
        }
    }

    return results;
} 