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
        
        const outputDirName = path.basename(baseOutputDir);
        const projectPath = outputDirName.toLowerCase() === projectName.toLowerCase()
            ? baseOutputDir
            : path.join(baseOutputDir, projectName);

        
        const config = getGlobalConfig();
        const basePackage = config.getBasePackage();
        const fullPackage = `${basePackage}.${projectName.toLowerCase()}`;

        
        const javaSrcPath = path.join(...JAVA_SRC_PATH);

        
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
                
                const config = getGlobalConfig();
                const basePackage = config.getBasePackage();
                const packageParts = basePackage.split('.');
                const microservicesPath = path.join(projectPath, 'src', 'main', 'java', ...packageParts, path.basename(projectPath), 'microservices');
                try {
                    const existingAggregates = await fs.readdir(microservicesPath);
                    if (existingAggregates.length > 0) {
                        // Preserve existing project directory
                    } else {
                        await fs.rm(projectPath, { recursive: true, force: true });
                    }
                } catch (microservicesErr) {
                    await fs.rm(projectPath, { recursive: true, force: true });
                }
            }
        } catch (accessErr) {
            // Directory does not exist yet
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
