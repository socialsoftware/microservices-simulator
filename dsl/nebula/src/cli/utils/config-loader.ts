import * as fs from 'fs';
import * as path from 'path';
import chalk from 'chalk';
import { GenerationConfig, initializeConfig, getGlobalConfig } from '../generators/common/config.js';

export interface NebulaConfig {
    projectName?: string;
    basePackage?: string;
    version?: string;
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
    framework?: {
        groupId?: string;
        artifactId?: string;
        version?: string;
    };
    portRange?: {
        min?: number;
        max?: number;
    };
}

export class ConfigLoader {
    private static readonly CONFIG_FILE_NAMES = ['nebula.config.json', 'nebula.json', '.nebularc.json'];

    

    private static resolveEnvironmentVariables(config: any): any {
        if (typeof config === 'string') {
            
            return config.replace(/\$\{([^}]+)\}/g, (match, varName) => {
                const envValue = process.env[varName];
                if (envValue === undefined) {
                    console.warn(chalk.yellow(`[WARN] Environment variable ${varName} is not set, using placeholder`));
                    return match; 
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

    

    private static validateSensitiveConfig(config: GenerationConfig): void {
        
        if (config.database?.password) {
            const password = config.database.password;


            if (password.includes('${') && password.includes('}')) {
                console.warn(chalk.yellow('[WARN] Database password contains unresolved environment variable placeholder'));
            }

        }
    }

    

    static async loadConfig(searchPath?: string, cliOptions?: Partial<GenerationConfig>): Promise<GenerationConfig> {
        const configFile = this.findConfigFile(searchPath);
        let fileConfig: NebulaConfig = {};

        if (configFile) {
            try {
                const configContent = fs.readFileSync(configFile, 'utf-8');
                fileConfig = JSON.parse(configContent);

                
                fileConfig = this.resolveEnvironmentVariables(fileConfig);
            } catch (error) {
                console.warn(chalk.yellow(`[WARN] Failed to parse config file: ${error instanceof Error ? error.message : String(error)}`));
            }
        }

        
        const generationConfig = this.convertToGenerationConfig(fileConfig);

        
        const mergedConfig = {
            ...generationConfig,
            ...cliOptions
        };

        
        this.validateSensitiveConfig(mergedConfig as GenerationConfig);

        
        initializeConfig(mergedConfig);

        return getGlobalConfig().getConfig();
    }

    

    private static findConfigFile(startPath?: string): string | null {
        const searchDir = startPath || process.cwd();

        
        for (const fileName of this.CONFIG_FILE_NAMES) {
            const configPath = path.join(searchDir, fileName);
            if (fs.existsSync(configPath)) {
                return configPath;
            }
        }

        
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

    

    private static convertToGenerationConfig(config: NebulaConfig): Partial<GenerationConfig> {
        const result: Partial<GenerationConfig> = {};

        if (config.projectName) {
            result.projectName = config.projectName;
        }

        if (config.version) {
            result.version = config.version;
        }

        if (config.basePackage) {
            result.basePackage = config.basePackage;
            
            if (config.projectName) {
                result.packageName = `${config.basePackage}.${config.projectName.toLowerCase()}`;
            }
        }

        if (config.outputDirectory) {
            result.outputDirectory = config.outputDirectory;
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

        if (config.framework) {
            result.simulatorFramework = {
                groupId: config.framework.groupId || 'pt.ulisboa.tecnico.socialsoftware',
                artifactId: config.framework.artifactId || 'MicroservicesSimulator',
                version: config.framework.version || '3.0.0-SNAPSHOT'
            };
        }

        if (config.portRange) {
            result.portRange = {
                min: config.portRange.min || 8080,
                max: config.portRange.max || 9999
            };
        }

        return result;
    }

    

    static async createDefaultConfig(projectName: string, basePackage?: string): Promise<void> {
        const defaultConfig: NebulaConfig = {
            projectName: projectName,
            basePackage: basePackage || 'com.example',
            version: '1.0.0-SNAPSHOT',
            outputDirectory: '../../applications',
            database: {
                type: 'postgresql',
                host: 'localhost',
                port: 5432,
                name: `${projectName}db`,
                username: 'postgres',
                password: '${NEBULA_DB_PASSWORD}' 
            },
            java: {
                version: '21',
                springBootVersion: '3.3.9'
            },
            framework: {
                groupId: 'pt.ulisboa.tecnico.socialsoftware',
                artifactId: 'MicroservicesSimulator',
                version: '3.0.0-SNAPSHOT'
            },
            portRange: {
                min: 8080,
                max: 9999
            }
        };

        const configPath = path.join(process.cwd(), 'nebula.config.json');
        const configContent = JSON.stringify(defaultConfig, null, 2);

        fs.writeFileSync(configPath, configContent);
        console.log(`Created default configuration file: ${configPath}`);
    }
}
