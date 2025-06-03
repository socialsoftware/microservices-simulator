import * as path from 'node:path';
import * as fs from 'node:fs/promises';
import { NebulaLanguageMetaData } from "../language/generated/module.js";

/**
 * Collects absolute paths of Nebula files from a given input path.
 * If the path is a file it returns the single-element array.
 * If the path is a directory it returns all direct children whose file extension
 * matches the Nebula language extensions.  By default the scan is non-recursive
 * to replicate current behaviour.  Set `recursive` to true for deep traversal.
 */
export async function collectNebulaFiles(inputPath: string, recursive = false): Promise<string[]> {
    const resolvedPath = path.resolve(process.cwd(), inputPath);
    const stats = await fs.stat(resolvedPath);

    const nebulaExtensions = NebulaLanguageMetaData.fileExtensions;

    const matchesExtension = (file: string) =>
        nebulaExtensions.some((ext) => file.endsWith(ext));

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