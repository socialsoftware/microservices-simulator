export interface FrameworkAnnotations {
    service: string;
    repository: string;
    component: string;
    transactional: string;
    autowired: string;
    inject: string;
    controller: string;
    restController: string;
}

export interface GenerationConfig {
    projectName: string;
    outputDirectory: string;
    packageName: string;
    basePackage: string; // New field for base package (e.g., com.company or pt.ulisboa.tecnico.socialsoftware)

    architecture: 'microservices' | 'causal-saga' | 'monolith';
    features: GenerationFeature[];
    consistencyModels?: string[];

    templateEngine: 'simple' | 'handlebars';
    templateCaching: boolean;

    strictMode: boolean;
    validateArchitecture: boolean;

    generateDocumentation: boolean;
    generateTests: boolean;

    javaVersion: string;
    springBootVersion: string;
    useJakarta: boolean;

    framework: 'spring' | 'jakarta' | 'custom';
    annotations: FrameworkAnnotations;

    transactionModel: 'SAGAS';

    database: {
        type: 'postgresql' | 'mysql' | 'mongodb' | 'h2';
        port: number;
        host: string;
        name: string;
        username: string;
        password: string;
    };
}

export type GenerationFeature =
    | 'entities'
    | 'dtos'
    | 'services'
    | 'factories'
    | 'repositories'
    | 'events'
    | 'coordination'
    | 'webapi'
    | 'validation'
    | 'saga'
    | 'integration';

export interface ArchitectureConfig {
    name: string;
    requiredFeatures: GenerationFeature[];
    optionalFeatures: GenerationFeature[];
    defaultTemplates: { [key: string]: string };
    validation: {
        requiresRootEntity: boolean;
        allowsMultipleRoots: boolean;
        requiredProperties: string[];
        requiredMethods: string[];
    };
}

export class ConfigManager {
    private config: GenerationConfig;
    private architectures: Map<string, ArchitectureConfig>;

    constructor(initialConfig?: Partial<GenerationConfig>) {
        this.config = this.createDefaultConfig();
        this.architectures = new Map();
        this.initializeArchitectures();

        if (initialConfig) {
            this.updateConfig(initialConfig);
        }
    }

    getConfig(): GenerationConfig {
        return { ...this.config };
    }

    updateConfig(updates: Partial<GenerationConfig>): void {
        this.config = { ...this.config, ...updates };
    }

    getArchitectureConfig(architecture: string): ArchitectureConfig | undefined {
        return this.architectures.get(architecture);
    }

    registerArchitecture(config: ArchitectureConfig): void {
        this.architectures.set(config.name, config);
    }

    validateConfig(): string[] {
        const errors: string[] = [];

        if (!this.config.projectName) {
            errors.push('Project name is required');
        }

        if (!this.config.outputDirectory) {
            errors.push('Output directory is required');
        }

        const archConfig = this.getArchitectureConfig(this.config.architecture);
        if (!archConfig) {
            errors.push(`Unknown architecture: ${this.config.architecture}`);
        } else {
            const missingFeatures = archConfig.requiredFeatures.filter(
                feature => !this.config.features.includes(feature)
            );
            if (missingFeatures.length > 0) {
                errors.push(`Missing required features for ${this.config.architecture}: ${missingFeatures.join(', ')}`);
            }
        }

        return errors;
    }

    getTemplatePath(feature: GenerationFeature): string {
        const archConfig = this.getArchitectureConfig(this.config.architecture);
        const templateName = archConfig?.defaultTemplates[feature] || `${feature}.template`;
        return `./templates/${this.config.architecture}/${templateName}`;
    }

    private createDefaultConfig(): GenerationConfig {
        return {
            projectName: 'project',
            outputDirectory: '../../applications',
            packageName: 'com.generated.microservices',
            basePackage: 'com.generated', // Default base package
            architecture: 'causal-saga',
            features: ['entities', 'dtos', 'services', 'factories', 'repositories'],
            templateEngine: 'simple',
            templateCaching: true,
            strictMode: false,
            validateArchitecture: true,
            generateDocumentation: false,
            generateTests: false,
            javaVersion: '17',
            springBootVersion: '3.0.0',
            useJakarta: true,
            framework: 'spring',
            annotations: this.getSpringAnnotations(),
            transactionModel: 'SAGAS',
            database: {
                type: 'postgresql',
                port: 5432,
                host: 'postgres',
                name: 'defaultdb',
                username: 'postgres',
                password: 'password'
            }
        };
    }

    private getSpringAnnotations(): FrameworkAnnotations {
        return {
            service: '@Service',
            repository: '@Repository',
            component: '@Component',
            transactional: '@Transactional',
            autowired: '@Autowired',
            inject: '@Inject',
            controller: '@Controller',
            restController: '@RestController'
        };
    }

    private getJakartaAnnotations(): FrameworkAnnotations {
        return {
            service: '@Service',
            repository: '@Repository',
            component: '@Named',
            transactional: '@Transactional',
            autowired: '@Inject',
            inject: '@Inject',
            controller: '@Controller',
            restController: '@RestController'
        };
    }

    getFrameworkAnnotations(framework?: 'spring' | 'jakarta' | 'custom'): FrameworkAnnotations {
        const targetFramework = framework || this.config.framework;
        switch (targetFramework) {
            case 'jakarta':
                return this.getJakartaAnnotations();
            case 'spring':
            default:
                return this.getSpringAnnotations();
        }
    }

    private initializeArchitectures(): void {
        this.registerArchitecture({
            name: 'microservices',
            requiredFeatures: ['entities', 'dtos', 'services', 'repositories'],
            optionalFeatures: ['events', 'webapi', 'validation'],
            defaultTemplates: {
                entities: 'microservice-entity.template',
                dtos: 'microservice-dto.template',
                services: 'microservice-service.template',
                repositories: 'microservice-repository.template'
            },
            validation: {
                requiresRootEntity: true,
                allowsMultipleRoots: false,
                requiredProperties: ['id'],
                requiredMethods: []
            }
        });

        this.registerArchitecture({
            name: 'causal-saga',
            requiredFeatures: ['entities', 'dtos', 'services', 'factories', 'repositories', 'saga'],
            optionalFeatures: ['events', 'coordination', 'webapi', 'validation', 'integration'],
            defaultTemplates: {
                entities: 'causal-entity.template',
                dtos: 'causal-dto.template',
                services: 'causal-service.template',
                saga: 'saga-coordination.template'
            },
            validation: {
                requiresRootEntity: true,
                allowsMultipleRoots: false,
                requiredProperties: ['aggregateId', 'version'],
                requiredMethods: ['buildWorkflow']
            }
        });

        this.registerArchitecture({
            name: 'monolith',
            requiredFeatures: ['entities', 'services', 'repositories'],
            optionalFeatures: ['dtos', 'webapi', 'validation'],
            defaultTemplates: {
                entities: 'monolith-entity.template',
                services: 'monolith-service.template',
                repositories: 'monolith-repository.template'
            },
            validation: {
                requiresRootEntity: false,
                allowsMultipleRoots: true,
                requiredProperties: [],
                requiredMethods: []
            }
        });
    }

    getPackageComponents(): string[] {
        return this.config.packageName.split('.');
    }

    getFeaturePackage(feature: GenerationFeature): string {
        const basePackage = this.config.packageName;
        const projectName = this.config.projectName.toLowerCase();

        switch (feature) {
            case 'entities':
                return `${basePackage}.${projectName}.aggregate`;
            case 'dtos':
                return `${basePackage}.${projectName}.dto`;
            case 'services':
                return `${basePackage}.${projectName}.service`;
            case 'repositories':
                return `${basePackage}.${projectName}.repository`;
            case 'factories':
                return `${basePackage}.${projectName}.factory`;
            case 'events':
                return `${basePackage}.${projectName}.events`;
            case 'coordination':
                return `${basePackage}.${projectName}.coordination`;
            case 'webapi':
                return `${basePackage}.${projectName}.webapi`;
            case 'validation':
                return `${basePackage}.${projectName}.validation`;
            case 'saga':
                return `${basePackage}.${projectName}.sagas`;
            case 'integration':
                return `${basePackage}.${projectName}`;
            default:
                return `${basePackage}.${projectName}`;
        }
    }

    buildPackageName(projectName: string, ...subPackages: string[]): string {
        const basePackage = this.config.basePackage;
        const projectPackage = `${basePackage}.${projectName.toLowerCase()}`;

        if (subPackages.length === 0) {
            return projectPackage;
        }

        return `${projectPackage}.${subPackages.join('.')}`;
    }

    getBasePackage(): string {
        return this.config.basePackage;
    }

    getFeatureOutputPath(feature: GenerationFeature): string {
        const packageComponents = this.getFeaturePackage(feature).split('.');
        return packageComponents.join('/');
    }
}

let globalConfig: ConfigManager | null = null;

export function getGlobalConfig(): ConfigManager {
    if (!globalConfig) {
        globalConfig = new ConfigManager();
    }
    return globalConfig;
}

export function initializeConfig(config?: Partial<GenerationConfig>): ConfigManager {
    globalConfig = new ConfigManager(config);
    return globalConfig;
}
