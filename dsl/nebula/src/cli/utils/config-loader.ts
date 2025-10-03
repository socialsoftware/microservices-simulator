import * as fs from 'fs';
import * as path from 'path';
import { GenerationConfig, initializeConfig, getGlobalConfig } from '../generator/base/config.js';

export interface NebulaConfig {
    projectName?: string;
    basePackage?: string;
    outputDirectory?: string;
    architecture?: 'microservices' | 'causal-saga' | 'monolith';
    features?: string[];
    consistencyModels?: string[];
    database?: {
        type?: 'postgresql' | 'mysql' | 'mongodb' | 'h2';
        host?: string;
        port?: number;
        name?: string;
        username?: string;
        password?: string;
    };
    java?: {
        version?: string;
        springBootVersion?: string;
    };
}

export class ConfigLoader {
    private static readonly CONFIG_FILE_NAMES = ['nebula.config.json', 'nebula.json', '.nebularc.json'];

    /**
     * Load configuration from file and merge with CLI options
     */
    static async loadConfig(searchPath?: string, cliOptions?: Partial<GenerationConfig>): Promise<GenerationConfig> {
        const configFile = this.findConfigFile(searchPath);
        let fileConfig: NebulaConfig = {};

        if (configFile) {
            console.log(`Loading configuration from: ${configFile}`);
            try {
                const configContent = fs.readFileSync(configFile, 'utf-8');
                fileConfig = JSON.parse(configContent);
            } catch (error) {
                console.warn(`Warning: Failed to parse config file: ${error instanceof Error ? error.message : String(error)}`);
            }
        }

        // Convert NebulaConfig to GenerationConfig format
        const generationConfig = this.convertToGenerationConfig(fileConfig);

        // Merge configurations: CLI options override file config
        const mergedConfig = {
            ...generationConfig,
            ...cliOptions
        };

        // Initialize global config with merged values
        initializeConfig(mergedConfig);

        return getGlobalConfig().getConfig();
    }

    /**
     * Find config file in project hierarchy
     */
    private static findConfigFile(startPath?: string): string | null {
        const searchDir = startPath || process.cwd();

        // Check current directory first
        for (const fileName of this.CONFIG_FILE_NAMES) {
            const configPath = path.join(searchDir, fileName);
            if (fs.existsSync(configPath)) {
                return configPath;
            }
        }

        // Search up the directory tree
        let currentDir = searchDir;
        while (currentDir !== path.dirname(currentDir)) {
            currentDir = path.dirname(currentDir);
            for (const fileName of this.CONFIG_FILE_NAMES) {
                const configPath = path.join(currentDir, fileName);
                if (fs.existsSync(configPath)) {
                    return configPath;
                }
            }
        }

        return null;
    }

    /**
     * Convert NebulaConfig to GenerationConfig format
     */
    private static convertToGenerationConfig(config: NebulaConfig): Partial<GenerationConfig> {
        const result: Partial<GenerationConfig> = {};

        if (config.projectName) {
            result.projectName = config.projectName;
        }

        if (config.basePackage) {
            result.basePackage = config.basePackage;
            // Also set packageName for backward compatibility
            if (config.projectName) {
                result.packageName = `${config.basePackage}.${config.projectName.toLowerCase()}`;
            }
        }

        if (config.outputDirectory) {
            result.outputDirectory = config.outputDirectory;
        }

        if (config.architecture) {
            result.architecture = config.architecture;
        }

        if (config.features) {
            result.features = config.features as any[];
        }

        if (config.consistencyModels) {
            result.consistencyModels = config.consistencyModels;
        }

        if (config.database) {
            result.database = {
                type: config.database.type || 'postgresql',
                host: config.database.host || 'localhost',
                port: config.database.port || 5432,
                name: config.database.name || 'defaultdb',
                username: config.database.username || 'postgres',
                password: config.database.password || 'password'
            };
        }

        if (config.java) {
            if (config.java.version) {
                result.javaVersion = config.java.version;
            }
            if (config.java.springBootVersion) {
                result.springBootVersion = config.java.springBootVersion;
            }
        }

        return result;
    }

    /**
     * Create a default config file in the current directory
     */
    static async createDefaultConfig(projectName: string, basePackage?: string): Promise<void> {
        const defaultConfig: NebulaConfig = {
            projectName: projectName,
            basePackage: basePackage || 'com.example',
            outputDirectory: '../../applications',
            architecture: 'causal-saga',
            features: ['entities', 'dtos', 'services', 'factories', 'repositories', 'webapi', 'saga'],
            database: {
                type: 'postgresql',
                host: 'localhost',
                port: 5432,
                name: `${projectName}db`,
                username: 'postgres',
                password: 'password'
            },
            java: {
                version: '17',
                springBootVersion: '3.0.0'
            }
        };

        const configPath = path.join(process.cwd(), 'nebula.config.json');
        const configContent = JSON.stringify(defaultConfig, null, 2);

        fs.writeFileSync(configPath, configContent);
        console.log(`Created default configuration file: ${configPath}`);
    }
}
