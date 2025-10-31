import * as fs from "node:fs/promises";
import * as path from "node:path";
import { ProjectPaths, JAVA_SRC_PATH } from "./types.js";
import { getGlobalConfig } from "../generators/common/config.js";

export class ProjectSetup {
    static async setupProjectPaths(
        baseOutputDir: string,
        inputPath: string,
        projectName: string
    ): Promise<ProjectPaths> {
        const projectPath = path.join(baseOutputDir, projectName);

        // Use configuration for all paths instead of hardcoded values
        const config = getGlobalConfig();
        const basePackage = config.getBasePackage();
        const fullPackage = `${basePackage}.${projectName.toLowerCase()}`;

        // Standard Maven directory structure (could be made configurable if needed)
        const javaSrcPath = path.join(...JAVA_SRC_PATH);

        // Convert package name to directory path (e.g., "pt.ulisboa.tecnico" → "pt/ulisboa/tecnico")
        const packagePath = fullPackage.split('.').join(path.sep);
        const javaPath = path.join(projectPath, javaSrcPath, packagePath);

        await this.handleExistingProject(projectPath, inputPath);

        await fs.mkdir(projectPath, { recursive: true });

        return {
            projectPath,
            javaPath,
            javaSrcPath,
            packagePath
        };
    }

    private static async handleExistingProject(projectPath: string, inputPath: string): Promise<void> {
        try {
            await fs.access(projectPath);
            const isSingleFile = inputPath.endsWith('.nebula');

            if (isSingleFile) {
                // Use configurable package path instead of hardcoded
                const config = getGlobalConfig();
                const basePackage = config.getBasePackage();
                const packageParts = basePackage.split('.');
                const microservicesPath = path.join(projectPath, 'src', 'main', 'java', ...packageParts, path.basename(projectPath), 'microservices');
                try {
                    const existingAggregates = await fs.readdir(microservicesPath);
                    if (existingAggregates.length > 0) {
                        console.log(`Preserving existing project directory with ${existingAggregates.length} aggregates: ${projectPath}`);
                    } else {
                        await fs.rm(projectPath, { recursive: true, force: true });
                        console.log(`Cleaned existing project directory: ${projectPath}`);
                    }
                } catch (microservicesErr) {
                    await fs.rm(projectPath, { recursive: true, force: true });
                    console.log(`Cleaned existing project directory: ${projectPath}`);
                }
            } else {
                console.log(`Using existing project directory: ${projectPath}`);
            }
        } catch (accessErr) {
            console.log(`Directory ${projectPath} did not exist`);
        }
    }

    static deriveProjectName(inputPath: string): string {
        const resolvedInputPath = path.resolve(inputPath);
        const inputFolderName = path.basename(resolvedInputPath);

        return inputFolderName.endsWith('.nebula')
            ? path.basename(path.dirname(resolvedInputPath))
            : inputFolderName;
    }
}
