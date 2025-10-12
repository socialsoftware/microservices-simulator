import * as fs from 'fs';
import * as path from 'path';
import { getGlobalConfig } from '../generators/shared/config.js';

/**
 * Utility to refactor hardcoded package names to use dynamic configuration
 * This helps migrate from hardcoded 'pt.ulisboa.tecnico.socialsoftware' to configurable base packages
 */
export class PackageNameRefactor {
    private static readonly HARDCODED_PACKAGE = 'pt.ulisboa.tecnico.socialsoftware';

    /**
     * Generate dynamic package name using configuration
     */
    static buildPackageName(projectName: string, ...subPackages: string[]): string {
        const config = getGlobalConfig();
        const basePackage = config.getBasePackage();
        const projectPackage = `${basePackage}.${projectName.toLowerCase()}`;

        if (subPackages.length === 0) {
            return projectPackage;
        }

        return `${projectPackage}.${subPackages.join('.')}`;
    }

    /**
     * Build microservice-specific package name
     */
    static buildMicroservicePackage(projectName: string, aggregateName: string, subPackage?: string): string {
        const parts = ['microservices', aggregateName.toLowerCase()];
        if (subPackage) {
            parts.push(subPackage);
        }
        return this.buildPackageName(projectName, ...parts);
    }

    /**
     * Build saga-specific package name
     */
    static buildSagaPackage(projectName: string, sagaType: string, ...subPackages: string[]): string {
        const parts = ['sagas', sagaType.toLowerCase(), ...subPackages];
        return this.buildPackageName(projectName, ...parts);
    }

    /**
     * Build web API package name
     */
    static buildWebApiPackage(projectName: string, ...subPackages: string[]): string {
        const parts = ['coordination', 'webapi', ...subPackages];
        return this.buildPackageName(projectName, ...parts);
    }

    /**
     * Replace hardcoded package references in a string
     */
    static replaceHardcodedPackage(content: string, projectName: string): string {
        const config = getGlobalConfig();
        const basePackage = config.getBasePackage();

        // Replace direct references
        content = content.replace(
            new RegExp(`${this.HARDCODED_PACKAGE}\\.\\$\\{projectName\\.toLowerCase\\(\\)\\}`, 'g'),
            `${basePackage}.\${projectName.toLowerCase()}`
        );

        // Replace string literals with projectName
        content = content.replace(
            new RegExp(`${this.HARDCODED_PACKAGE}\\.${projectName.toLowerCase()}`, 'g'),
            `${basePackage}.${projectName.toLowerCase()}`
        );

        // Replace template literals
        content = content.replace(
            new RegExp(`\\$\\{this\\.getBasePackage\\(\\)\\}`, 'g'),
            `\${this.getBasePackage()}`
        );

        return content;
    }

    /**
     * Update a file to use dynamic package names
     */
    static async updateFile(filePath: string): Promise<void> {
        try {
            let content = fs.readFileSync(filePath, 'utf-8');

            // Check if file contains hardcoded package
            if (!content.includes(this.HARDCODED_PACKAGE)) {
                return;
            }

            console.log(`Updating file: ${filePath}`);

            // Replace hardcoded package name patterns
            content = content.replace(
                /pt\.ulisboa\.tecnico\.socialsoftware/g,
                '${this.getBasePackage()}'
            );

            // For TypeScript files, ensure proper imports
            if (filePath.endsWith('.ts')) {
                // Add import if not present
                if (!content.includes('import { getGlobalConfig }') && !content.includes('OrchestrationBase')) {
                    const importStatement = `import { getGlobalConfig } from '../generators/shared/config.js';\n`;

                    // Find the right place to add import (after other imports)
                    const importRegex = /^import .* from .*;\n/gm;
                    const lastImport = content.match(importRegex);
                    if (lastImport && lastImport.length > 0) {
                        const lastImportIndex = content.lastIndexOf(lastImport[lastImport.length - 1]);
                        const insertPosition = lastImportIndex + lastImport[lastImport.length - 1].length;
                        content = content.slice(0, insertPosition) + importStatement + content.slice(insertPosition);
                    } else {
                        // If no imports found, add at the beginning
                        content = importStatement + content;
                    }
                }

                // Update package name construction to use config
                content = content.replace(
                    /const packageName = `pt\.ulisboa\.tecnico\.socialsoftware\.\$\{(.+?)\}`;/g,
                    'const config = getGlobalConfig();\n    const packageName = config.buildPackageName($1);'
                );
            }

            fs.writeFileSync(filePath, content);
            console.log(`✓ Updated ${filePath}`);

        } catch (error) {
            console.error(`Error updating file ${filePath}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }

    /**
     * Update all generator files in a directory
     */
    static async updateGenerators(baseDir: string): Promise<void> {
        const generatorDirs = [
            path.join(baseDir, 'generator/impl/entity'),
            path.join(baseDir, 'generator/impl/service'),
            path.join(baseDir, 'generator/impl/repository'),
            path.join(baseDir, 'generator/impl/saga'),
            path.join(baseDir, 'generator/impl/web'),
            path.join(baseDir, 'generator/impl/testing'),
            path.join(baseDir, 'generator/base'),
            path.join(baseDir, 'core')
        ];

        for (const dir of generatorDirs) {
            if (!fs.existsSync(dir)) {
                console.log(`Directory not found: ${dir}`);
                continue;
            }

            const files = fs.readdirSync(dir).filter(f => f.endsWith('.ts'));

            for (const file of files) {
                const filePath = path.join(dir, file);
                await this.updateFile(filePath);
            }
        }
    }

    /**
     * Generate sample usage for migrating a generator
     */
    static generateMigrationExample(): string {
        return `
// Before:
const packageName = \`pt.ulisboa.tecnico.socialsoftware.\${projectName.toLowerCase()}.microservices.\${aggregateName.toLowerCase()}.aggregate\`;

// After:
import { getGlobalConfig } from '../generators/shared/config.js';

const config = getGlobalConfig();
const packageName = config.buildPackageName(
    projectName, 
    'microservices', 
    aggregateName.toLowerCase(), 
    'aggregate'
);

// Or using the utility:
import { PackageNameRefactor } from '../utils/package-name-refactor.js';

const packageName = PackageNameRefactor.buildMicroservicePackage(
    projectName, 
    aggregateName, 
    'aggregate'
);
`;
    }
}
