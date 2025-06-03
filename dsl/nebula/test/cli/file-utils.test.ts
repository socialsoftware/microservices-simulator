import { describe, expect, test, beforeAll, afterAll } from "vitest";
import { collectNebulaFiles } from "../../src/cli/file-utils.js";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import * as os from "node:os";

let tempDir: string;
let fileA: string;
let fileB: string;

beforeAll(async () => {
    tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "nebula-test-"));
    fileA = path.join(tempDir, "a.nebula");
    fileB = path.join(tempDir, "b.nebula");
    await fs.writeFile(fileA, "Aggregate A {}");
    await fs.writeFile(fileB, "Aggregate B {}");
});

afterAll(async () => {
    await fs.rm(tempDir, { recursive: true, force: true });
});

describe("collectNebulaFiles", () => {
    test("collects nebula files from directory (non-recursive)", async () => {
        const files = await collectNebulaFiles(tempDir);
        // Order is not guaranteed, so use set comparison
        expect(new Set(files)).toEqual(new Set([fileA, fileB]));
    });

    test("collects single nebula file when path is a file", async () => {
        const files = await collectNebulaFiles(fileA);
        expect(files).toEqual([fileA]);
    });
}); 