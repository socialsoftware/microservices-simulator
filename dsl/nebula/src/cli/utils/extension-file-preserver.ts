import * as fs from 'node:fs/promises';
import * as path from 'node:path';

export class ExtensionFilePreserver {

    static async snapshot(rootDir: string): Promise<Map<string, string>> {
        const snapshot = new Map<string, string>();

        try {
            await fs.access(rootDir);
        } catch {
            return snapshot;
        }

        await this.walk(rootDir, snapshot);
        return snapshot;
    }

    static async restore(snapshot: Map<string, string>): Promise<void> {
        for (const [absolutePath, content] of snapshot) {
            await fs.mkdir(path.dirname(absolutePath), { recursive: true });
            await fs.writeFile(absolutePath, content, 'utf-8');
        }
    }

    private static async walk(dir: string, acc: Map<string, string>): Promise<void> {
        let entries: import('node:fs').Dirent[];
        try {
            entries = await fs.readdir(dir, { withFileTypes: true });
        } catch {
            return;
        }

        for (const entry of entries) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                await this.walk(full, acc);
            } else if (entry.isFile() && entry.name.endsWith('ServiceExtension.java')) {
                const content = await fs.readFile(full, 'utf-8');
                acc.set(full, content);
            }
        }
    }
}
