import * as fs from 'fs';
import * as path from 'path';
import { GenerationConfig, initializeConfig, getGlobalConfig } from '../generators/common/config.js';

export interface NebulaConfig {
    projectName?: string;
    basePackage?: string;
    outputDirectory?: string;
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
     * Resolve environment variable placeholders in configuration values
     */
    private static resolveEnvironmentVariables(config: any): any {
        if (typeof config === 'string') {
            // Replace ${VAR_NAME} with environment variable value
            return config.replace(/\$\{([^}]+)\}/g, (match, varName) => {
                const envValue = process.env[varName];
                if (envValue === undefined) {
                    console.warn(`Environment variable ${varName} is not set, using placeholder`);
                    return match; // Keep placeholder if env var not found
                }
                return envValue;
            });
        } else if (Array.isArray(config)) {
            return config.map(item => this.resolveEnvironmentVariables(item));
        } else if (config && typeof config === 'object') {
            const resolved: any = {};
            for (const [key, value] of Object.entries(config)) {
                resolved[key] = this.resolveEnvironmentVariables(value);
            }
            return resolved;
        }
        return config;
    }

    /**
     * Validate sensitive configuration values
     */
    private static validateSensitiveConfig(config: GenerationConfig): void {
        // Check for insecure passwords
        if (config.database?.password) {
            const password = config.database.password;

            // Check if password is still a placeholder
            if (password.includes('${') && password.includes('}')) {
                console.warn('⚠️  Database password contains unresolved environment variable placeholder');
            }

            // Check for weak passwords
            const weakPasswords = ['password', '123456', 'admin', 'root', 'test', ''];
            if (weakPasswords.includes(password.toLowerCase())) {
                console.warn('⚠️  Database password appears to be weak or default. Consider using a stronger password.');
            }

            // Don't log the actual password
            console.log('Database password configured (not displayed for security)');
        }

        // Validate other security-sensitive settings
        if (config.database?.host && config.database.host !== 'localhost' && config.database.host !== '127.0.0.1') {
            console.log(`Database host configured: ${config.database.host}`);
        }
    }

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

                // Resolve environment variables in the loaded config
                fileConfig = this.resolveEnvironmentVariables(fileConfig);
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

        // Validate sensitive configuration
        this.validateSensitiveConfig(mergedConfig as GenerationConfig);

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

        // Architecture and features removed - always generate everything

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
            database: {
                type: 'postgresql',
                host: 'localhost',
                port: 5432,
                name: `${projectName}db`,
                username: 'postgres',
                password: '${NEBULA_DB_PASSWORD}' // Environment variable placeholder
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
