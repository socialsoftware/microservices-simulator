


import chalk from 'chalk';
import { getGlobalConfig, ConfigManager } from "../generators/common/config.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "./error-handler.js";



export interface ProjectConfig {
    projectName: string;
    basePackage: string;
    architecture: string;
    outputPath: string;
    javaVersion: string;
}



export interface DatabaseConfig {
    type: string;
    host: string;
    port: number;
    name: string;
    username: string;
    password: string;
    jdbcUrl: string;
    driverClass: string;
    dialect: string;
}



export interface GenerationConfig {
    enableValidation: boolean;
    enableCaching: boolean;
    enableParallelGeneration: boolean;
    enableDevMode: boolean;
    templateRoot: string;
    outputFormat: string;
}



export interface PackageConfig {
    basePackage: string;
    microservicesPackage: string;
    coordinationPackage: string;
    sagasPackage: string;
    sharedPackage: string;
}



export interface FrameworkConfig {
    transactionModel: string;
    enableRetry: boolean;
    enableSagas: boolean;
    enableEvents: boolean;
    enableValidation: boolean;
}



export class ConfigurationFacade {
    private static instance: ConfigurationFacade;
    private configManager: ConfigManager;
    private cachedConfigs: Map<string, any> = new Map();

    private constructor() {
        this.configManager = getGlobalConfig();
    }

    

    static getInstance(): ConfigurationFacade {
        if (!ConfigurationFacade.instance) {
            ConfigurationFacade.instance = new ConfigurationFacade();
        }
        return ConfigurationFacade.instance;
    }

    

    getProjectConfig(): ProjectConfig {
        const cacheKey = 'project-config';

        if (this.cachedConfigs.has(cacheKey)) {
            return this.cachedConfigs.get(cacheKey);
        }

        const rawConfig = this.configManager.getConfig();
        const config: ProjectConfig = {
            projectName: rawConfig.projectName,
            basePackage: this.configManager.getBasePackage(),
            architecture: rawConfig.architecture,
            outputPath: rawConfig.outputDirectory,
            javaVersion: rawConfig.javaVersion || '21'
        };

        this.cachedConfigs.set(cacheKey, config);
        return config;
    }

    

    getDatabaseConfig(projectName?: string): DatabaseConfig {
        const cacheKey = `database-config-${projectName || 'default'}`;

        if (this.cachedConfigs.has(cacheKey)) {
            return this.cachedConfigs.get(cacheKey);
        }

        const config: DatabaseConfig = {
            type: 'postgresql',
            host: 'localhost',
            port: 5432,
            name: projectName ? projectName.toLowerCase() : 'defaultdb',
            username: 'postgres',
            password: 'postgres',
            jdbcUrl: this.buildJdbcUrl(projectName),
            driverClass: this.getDatabaseDriverClass('postgresql'),
            dialect: this.getDatabaseDialect('postgresql')
        };

        this.cachedConfigs.set(cacheKey, config);
        return config;
    }

    

    getGenerationConfig(): GenerationConfig {
        const cacheKey = 'generation-config';

        if (this.cachedConfigs.has(cacheKey)) {
            return this.cachedConfigs.get(cacheKey);
        }

        const config: GenerationConfig = {
            enableValidation: true,
            enableCaching: true,
            enableParallelGeneration: true,
            enableDevMode: process.env.NODE_ENV === 'development',
            templateRoot: this.resolveTemplateRoot(),
            outputFormat: 'java'
        };

        this.cachedConfigs.set(cacheKey, config);
        return config;
    }

    

    getPackageConfig(): PackageConfig {
        const cacheKey = 'package-config';

        if (this.cachedConfigs.has(cacheKey)) {
            return this.cachedConfigs.get(cacheKey);
        }

        const basePackage = this.configManager.getBasePackage();
        const config: PackageConfig = {
            basePackage,
            microservicesPackage: `${basePackage}.microservices`,
            coordinationPackage: `${basePackage}.coordination`,
            sagasPackage: `${basePackage}.sagas`,
            sharedPackage: `${basePackage}.shared`
        };

        this.cachedConfigs.set(cacheKey, config);
        return config;
    }

    

    getFrameworkConfig(): FrameworkConfig {
        const cacheKey = 'framework-config';

        if (this.cachedConfigs.has(cacheKey)) {
            return this.cachedConfigs.get(cacheKey);
        }

        const config: FrameworkConfig = {
            transactionModel: 'SAGAS',
            enableRetry: true,
            enableSagas: true,
            enableEvents: true,
            enableValidation: true
        };

        this.cachedConfigs.set(cacheKey, config);
        return config;
    }

    

    buildPackageName(projectName: string, ...subPackages: string[]): string {
        return this.configManager.buildPackageName(projectName, ...subPackages);
    }

    buildMicroservicePackage(projectName: string, aggregate: string, component: string): string {
        return this.buildPackageName(projectName, 'microservices', aggregate.toLowerCase(), component);
    }

    buildCoordinationPackage(projectName: string, component: string): string {
        return this.buildPackageName(projectName, 'coordination', component);
    }

    buildSagaPackage(projectName: string, component: string): string {
        return this.buildPackageName(projectName, 'sagas', component);
    }

    buildSharedPackage(projectName: string, component: string): string {
        return this.buildPackageName(projectName, 'shared', component);
    }

    

    validateConfiguration(): ValidationResult {
        const errors: string[] = [];
        const warnings: string[] = [];

        try {
            
            const projectConfig = this.getProjectConfig();
            if (!projectConfig.projectName) {
                errors.push('Project name is required');
            }
            if (!projectConfig.basePackage) {
                errors.push('Base package is required');
            }
            if (!projectConfig.outputPath) {
                errors.push('Output path is required');
            }

            
            const dbConfig = this.getDatabaseConfig();
            if (!dbConfig.type) {
                errors.push('Database type is required');
            }
            if (!dbConfig.host) {
                warnings.push('Database host not specified, using default');
            }

            
            const genConfig = this.getGenerationConfig();
            if (!genConfig.templateRoot) {
                warnings.push('Template root not specified, using default');
            }

        } catch (error) {
            errors.push(`Configuration validation failed: ${error instanceof Error ? error.message : String(error)}`);
        }

        return {
            isValid: errors.length === 0,
            errors,
            warnings
        };
    }

    

    override(overrides: Partial<{
        project: Partial<ProjectConfig>;
        database: Partial<DatabaseConfig>;
        generation: Partial<GenerationConfig>;
        package: Partial<PackageConfig>;
        framework: Partial<FrameworkConfig>;
    }>): void {
        
        if (overrides.project) {
            this.cachedConfigs.delete('project-config');
        }
        if (overrides.database) {
            this.cachedConfigs.delete('database-config-default');
        }
        if (overrides.generation) {
            this.cachedConfigs.delete('generation-config');
        }
        if (overrides.package) {
            this.cachedConfigs.delete('package-config');
        }
        if (overrides.framework) {
            this.cachedConfigs.delete('framework-config');
        }

    }

    

    resetCache(): void {
        this.cachedConfigs.clear();
    }

    

    getConfigurationSummary(): Record<string, any> {
        return {
            project: this.getProjectConfig(),
            database: this.getDatabaseConfig(),
            generation: this.getGenerationConfig(),
            package: this.getPackageConfig(),
            framework: this.getFrameworkConfig()
        };
    }

    

    private buildJdbcUrl(projectName?: string): string {
        const dbName = projectName ? projectName.toLowerCase() : 'defaultdb';
        return `jdbc:postgresql://postgres:5432/${dbName}`;
    }

    private getDatabaseDriverClass(type: string): string {
        switch (type) {
            case 'postgresql': return 'org.postgresql.Driver';
            case 'mysql': return 'com.mysql.cj.jdbc.Driver';
            case 'h2': return 'org.h2.Driver';
            default: return 'org.postgresql.Driver';
        }
    }

    private getDatabaseDialect(type: string): string {
        switch (type) {
            case 'postgresql': return 'org.hibernate.dialect.PostgreSQLDialect';
            case 'mysql': return 'org.hibernate.dialect.MySQLDialect';
            case 'h2': return 'org.hibernate.dialect.H2Dialect';
            default: return 'org.hibernate.dialect.PostgreSQLDialect';
        }
    }

    private resolveTemplateRoot(): string {
        
        return './src/cli/templates';
    }
}



interface ValidationResult {
    isValid: boolean;
    errors: string[];
    warnings: string[];
}



export class ConfigUtils {
    private static facade = ConfigurationFacade.getInstance();

    

    static getProjectName(): string {
        return this.facade.getProjectConfig().projectName;
    }

    static getBasePackage(): string {
        return this.facade.getProjectConfig().basePackage;
    }

    static getArchitecture(): string {
        return this.facade.getProjectConfig().architecture;
    }


    static getOutputPath(): string {
        return this.facade.getProjectConfig().outputPath;
    }

    

    static getDatabaseUrl(projectName?: string): string {
        return this.facade.getDatabaseConfig(projectName).jdbcUrl;
    }

    static getDatabaseType(): string {
        return this.facade.getDatabaseConfig().type;
    }

    

    static isDevMode(): boolean {
        return this.facade.getGenerationConfig().enableDevMode;
    }

    static isCachingEnabled(): boolean {
        return this.facade.getGenerationConfig().enableCaching;
    }

    static isParallelGenerationEnabled(): boolean {
        return this.facade.getGenerationConfig().enableParallelGeneration;
    }

    

    static buildPackage(projectName: string, ...segments: string[]): string {
        return this.facade.buildPackageName(projectName, ...segments);
    }

    static buildMicroservicePackage(projectName: string, aggregate: string, component: string): string {
        return this.facade.buildMicroservicePackage(projectName, aggregate, component);
    }

    

    static getTransactionModel(): string {
        return this.facade.getFrameworkConfig().transactionModel;
    }


    

    static validateConfig(): ValidationResult {
        return this.facade.validateConfiguration();
    }

    static ensureValidConfig(): void {
        const validation = this.validateConfig();
        if (!validation.isValid) {
            ErrorHandler.handle(
                new Error(`Configuration validation failed: ${validation.errors.join(', ')}`),
                ErrorUtils.aggregateContext(
                    'validate configuration',
                    'system',
                    'configuration-facade',
                    { errors: validation.errors, warnings: validation.warnings }
                ),
                ErrorSeverity.FATAL
            );
        }

        if (validation.warnings.length > 0) {
            validation.warnings.forEach(warning => {
                console.warn(chalk.yellow(`[WARN] Configuration warning: ${warning}`));
            });
        }
    }
}
