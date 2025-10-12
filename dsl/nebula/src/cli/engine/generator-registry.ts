import { EntityGenerator } from "../generators/microservices/entity/entity-orchestrator.js";
import { DtoGenerator } from "../generators/microservices/shared/dto-generator.js";
import { ServiceGenerator } from "../generators/microservices/service/default/main.js";
import { ServiceDefinitionGenerator } from "../generators/microservices/service/service-definition-generator.js";
import { FactoryGenerator } from "../generators/microservices/factory/factory-generator.js";
import { RepositoryGenerator } from "../generators/microservices/repository/repository-generator.js";
import { RepositoryInterfaceGenerator } from "../generators/microservices/repository/repository-interface-generator.js";
import { EventGenerator } from "../generators/microservices/events/event-generator.js";
import { CoordinationGenerator } from "../generators/coordination/index.js";
import { WebApiGenerator } from "../generators/coordination/webapi/webapi-generator.js";
import { ValidationGenerator } from "../generators/validation/validation-generator.js";
import { IntegrationGenerator } from "../generators/coordination/config/integration-generator.js";
import { SagaGenerator } from "../generators/sagas/saga-generator.js";
import { SagaFunctionalityGenerator } from "../generators/sagas/saga-functionality-generator.js";
import { ExceptionGenerator } from "../generators/common/exception-generator.js";
import { EventHandlerGenerator } from "../generators/microservices/events/event-handler-generator.js";
import { CausalEntityGenerator } from "../generators/sagas/causal-entity-generator.js";
import { ConfigurationGenerator } from "../generators/coordination/config/configuration-generator.js";
import { ValidationSystem } from "../generators/validation/validation-system.js";

/**
 * Generator metadata for discovery and management
 */
export interface GeneratorMetadata {
    name: string;
    version: string;
    description: string;
    category: 'microservices' | 'coordination' | 'validation' | 'sagas' | 'config';
    dependencies: string[];
    outputTypes: string[];
    isRequired: boolean;
}

/**
 * Enhanced generator interface with metadata
 */
export interface GeneratorInfo {
    instance: any;
    metadata: GeneratorMetadata;
    isEnabled: boolean;
}

/**
 * Generator discovery and registration system
 */
export class GeneratorDiscovery {
    private static generators = new Map<string, GeneratorInfo>();

    /**
     * Register a generator with metadata
     */
    static register(name: string, instance: any, metadata: GeneratorMetadata): void {
        this.generators.set(name, {
            instance,
            metadata,
            isEnabled: true
        });
    }

    /**
     * Get all registered generators
     */
    static getAll(): Map<string, GeneratorInfo> {
        return new Map(this.generators);
    }

    /**
     * Get generators by category
     */
    static getByCategory(category: GeneratorMetadata['category']): GeneratorInfo[] {
        return Array.from(this.generators.values())
            .filter(info => info.metadata.category === category);
    }

    /**
     * Get generator by name
     */
    static get(name: string): GeneratorInfo | undefined {
        return this.generators.get(name);
    }

    /**
     * Check if generator exists and is enabled
     */
    static isAvailable(name: string): boolean {
        const info = this.generators.get(name);
        return info !== undefined && info.isEnabled;
    }

    /**
     * Enable/disable a generator
     */
    static setEnabled(name: string, enabled: boolean): void {
        const info = this.generators.get(name);
        if (info) {
            info.isEnabled = enabled;
        }
    }

    /**
     * Validate generator dependencies
     */
    static validateDependencies(): { valid: boolean; errors: string[] } {
        const errors: string[] = [];

        for (const [name, info] of this.generators) {
            if (!info.isEnabled) continue;

            for (const dependency of info.metadata.dependencies) {
                if (!this.isAvailable(dependency)) {
                    errors.push(`Generator '${name}' requires '${dependency}' but it's not available`);
                }
            }
        }

        return { valid: errors.length === 0, errors };
    }

    /**
     * Get generation order based on dependencies
     */
    static getGenerationOrder(): string[] {
        const visited = new Set<string>();
        const visiting = new Set<string>();
        const order: string[] = [];

        const visit = (name: string): void => {
            if (visited.has(name)) return;
            if (visiting.has(name)) {
                throw new Error(`Circular dependency detected involving generator '${name}'`);
            }

            const info = this.generators.get(name);
            if (!info || !info.isEnabled) return;

            visiting.add(name);

            for (const dependency of info.metadata.dependencies) {
                visit(dependency);
            }

            visiting.delete(name);
            visited.add(name);
            order.push(name);
        };

        for (const name of this.generators.keys()) {
            visit(name);
        }

        return order;
    }
}

export interface GeneratorRegistry {
    entityGenerator: EntityGenerator;
    dtoGenerator: DtoGenerator;
    serviceGenerator: ServiceGenerator;
    serviceDefinitionGenerator: ServiceDefinitionGenerator;
    factoryGenerator: FactoryGenerator;
    repositoryGenerator: RepositoryGenerator;
    repositoryInterfaceGenerator: RepositoryInterfaceGenerator;

    eventGenerator: EventGenerator;
    coordinationGenerator: CoordinationGenerator;
    webApiGenerator: WebApiGenerator;
    validationGenerator: ValidationGenerator;

    integrationGenerator: IntegrationGenerator;
    configurationGenerator: ConfigurationGenerator;

    sagaGenerator: SagaGenerator;
    sagaFunctionalityGenerator: SagaFunctionalityGenerator;
    causalEntityGenerator: CausalEntityGenerator;

    exceptionGenerator: ExceptionGenerator;
    eventHandlerGenerator: EventHandlerGenerator;

    validationSystem: ValidationSystem;
}

export class GeneratorRegistryFactory {
    private static initialized = false;

    /**
     * Initialize all generators with metadata
     */
    private static initializeGenerators(): void {
        if (this.initialized) return;

        // Microservices generators
        GeneratorDiscovery.register('entityGenerator', new EntityGenerator(), {
            name: 'Entity Generator',
            version: '1.0.0',
            description: 'Generates JPA entities from DSL definitions',
            category: 'microservices',
            dependencies: [],
            outputTypes: ['java'],
            isRequired: true
        });

        GeneratorDiscovery.register('dtoGenerator', new DtoGenerator(), {
            name: 'DTO Generator',
            version: '1.0.0',
            description: 'Generates Data Transfer Objects',
            category: 'microservices',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: true
        });

        GeneratorDiscovery.register('serviceGenerator', new ServiceGenerator(), {
            name: 'Service Generator',
            version: '1.0.0',
            description: 'Generates service classes with business logic',
            category: 'microservices',
            dependencies: ['entityGenerator', 'dtoGenerator'],
            outputTypes: ['java'],
            isRequired: true
        });

        GeneratorDiscovery.register('serviceDefinitionGenerator', new ServiceDefinitionGenerator(), {
            name: 'Service Definition Generator',
            version: '1.0.0',
            description: 'Generates service definitions from DSL',
            category: 'microservices',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        GeneratorDiscovery.register('factoryGenerator', new FactoryGenerator(), {
            name: 'Factory Generator',
            version: '1.0.0',
            description: 'Generates factory classes for entity creation',
            category: 'microservices',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: true
        });

        GeneratorDiscovery.register('repositoryGenerator', new RepositoryGenerator(), {
            name: 'Repository Generator',
            version: '1.0.0',
            description: 'Generates JPA repository implementations',
            category: 'microservices',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: true
        });

        GeneratorDiscovery.register('repositoryInterfaceGenerator', new RepositoryInterfaceGenerator(), {
            name: 'Repository Interface Generator',
            version: '1.0.0',
            description: 'Generates repository interfaces',
            category: 'microservices',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: true
        });

        GeneratorDiscovery.register('eventGenerator', new EventGenerator(), {
            name: 'Event Generator',
            version: '1.0.0',
            description: 'Generates domain events',
            category: 'microservices',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        // Coordination generators
        GeneratorDiscovery.register('coordinationGenerator', new CoordinationGenerator(), {
            name: 'Coordination Generator',
            version: '1.0.0',
            description: 'Generates coordination layer components',
            category: 'coordination',
            dependencies: ['serviceGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        GeneratorDiscovery.register('webApiGenerator', new WebApiGenerator(), {
            name: 'Web API Generator',
            version: '1.0.0',
            description: 'Generates REST API controllers',
            category: 'coordination',
            dependencies: ['serviceGenerator', 'dtoGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        // Validation generators
        GeneratorDiscovery.register('validationGenerator', new ValidationGenerator(), {
            name: 'Validation Generator',
            version: '1.0.0',
            description: 'Generates validation logic and constraints',
            category: 'validation',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        GeneratorDiscovery.register('validationSystem', new ValidationSystem(), {
            name: 'Validation System',
            version: '1.0.0',
            description: 'Comprehensive validation system',
            category: 'validation',
            dependencies: ['validationGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        // Configuration generators
        GeneratorDiscovery.register('integrationGenerator', new IntegrationGenerator(), {
            name: 'Integration Generator',
            version: '1.0.0',
            description: 'Generates integration configurations',
            category: 'config',
            dependencies: [],
            outputTypes: ['java', 'properties', 'yml'],
            isRequired: false
        });

        GeneratorDiscovery.register('configurationGenerator', new ConfigurationGenerator(), {
            name: 'Configuration Generator',
            version: '1.0.0',
            description: 'Generates application configurations',
            category: 'config',
            dependencies: [],
            outputTypes: ['java', 'properties', 'yml'],
            isRequired: false
        });

        // Saga generators
        GeneratorDiscovery.register('sagaGenerator', new SagaGenerator(), {
            name: 'Saga Generator',
            version: '1.0.0',
            description: 'Generates saga orchestration logic',
            category: 'sagas',
            dependencies: ['entityGenerator', 'serviceGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        GeneratorDiscovery.register('sagaFunctionalityGenerator', new SagaFunctionalityGenerator(), {
            name: 'Saga Functionality Generator',
            version: '1.0.0',
            description: 'Generates saga functionality implementations',
            category: 'sagas',
            dependencies: ['sagaGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        GeneratorDiscovery.register('causalEntityGenerator', new CausalEntityGenerator(), {
            name: 'Causal Entity Generator',
            version: '1.0.0',
            description: 'Generates causal consistency entities',
            category: 'sagas',
            dependencies: ['entityGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        // Utility generators
        GeneratorDiscovery.register('exceptionGenerator', new ExceptionGenerator(), {
            name: 'Exception Generator',
            version: '1.0.0',
            description: 'Generates custom exception classes',
            category: 'microservices',
            dependencies: [],
            outputTypes: ['java'],
            isRequired: false
        });

        GeneratorDiscovery.register('eventHandlerGenerator', new EventHandlerGenerator(), {
            name: 'Event Handler Generator',
            version: '1.0.0',
            description: 'Generates event handler implementations',
            category: 'microservices',
            dependencies: ['eventGenerator'],
            outputTypes: ['java'],
            isRequired: false
        });

        this.initialized = true;
    }

    /**
     * Create registry using discovery system
     */
    static createRegistry(): GeneratorRegistry {
        this.initializeGenerators();

        // Validate dependencies
        const validation = GeneratorDiscovery.validateDependencies();
        if (!validation.valid) {
            console.warn('Generator dependency validation failed:');
            validation.errors.forEach(error => console.warn(`  - ${error}`));
        }

        // Create registry from discovered generators
        const registry: any = {};

        for (const [name, info] of GeneratorDiscovery.getAll()) {
            if (info.isEnabled) {
                registry[name] = info.instance;
            }
        }

        return registry as GeneratorRegistry;
    }

    /**
     * Create registry with only required generators
     */
    static createMinimalRegistry(): Partial<GeneratorRegistry> {
        this.initializeGenerators();

        const registry: any = {};

        for (const [name, info] of GeneratorDiscovery.getAll()) {
            if (info.isEnabled && info.metadata.isRequired) {
                registry[name] = info.instance;
            }
        }

        return registry;
    }

    /**
     * Get generators in dependency order
     */
    static getGenerationOrder(): string[] {
        this.initializeGenerators();
        return GeneratorDiscovery.getGenerationOrder();
    }

    /**
     * Enable/disable generators by category
     */
    static configureByCategory(category: GeneratorMetadata['category'], enabled: boolean): void {
        this.initializeGenerators();

        const generators = GeneratorDiscovery.getByCategory(category);
        generators.forEach(info => {
            GeneratorDiscovery.setEnabled(info.metadata.name, enabled);
        });
    }

    /**
     * Get registry statistics
     */
    static getStatistics(): {
        total: number;
        enabled: number;
        byCategory: Record<string, number>;
        required: number;
    } {
        this.initializeGenerators();

        const all = GeneratorDiscovery.getAll();
        const byCategory: Record<string, number> = {};
        let enabled = 0;
        let required = 0;

        for (const info of all.values()) {
            if (info.isEnabled) enabled++;
            if (info.metadata.isRequired) required++;

            byCategory[info.metadata.category] = (byCategory[info.metadata.category] || 0) + 1;
        }

        return {
            total: all.size,
            enabled,
            byCategory,
            required
        };
    }
}
