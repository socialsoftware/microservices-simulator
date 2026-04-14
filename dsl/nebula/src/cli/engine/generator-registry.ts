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

import { IntegrationGenerator } from "../generators/coordination/config/integration-generator.js";
import { SagaGenerator } from "../generators/sagas/saga-generator.js";
import { SagaFunctionalityGenerator } from "../generators/sagas/saga-functionality-generator.js";
import { ExceptionGenerator } from "../generators/common/exception-generator.js";
import { EventHandlerGenerator } from "../generators/microservices/events/event-handler-generator.js";

import { ConfigurationGenerator } from "../generators/coordination/config/configuration-generator.js";
import { AggregateValidator } from "../generators/validation/validation-system.js";



export interface GeneratorMetadata {
    name: string;
    version: string;
    description: string;
    category: 'microservices' | 'coordination' | 'validation' | 'sagas' | 'config';
    dependencies: string[];
    outputTypes: string[];
    isRequired: boolean;
}



export interface GeneratorInfo {
    instance: any;
    metadata: GeneratorMetadata;
    isEnabled: boolean;
}



export class GeneratorDiscovery {
    private static generators = new Map<string, GeneratorInfo>();

    

    static register(name: string, instance: any, metadata: GeneratorMetadata): void {
        this.generators.set(name, {
            instance,
            metadata,
            isEnabled: true
        });
    }

    

    static getAll(): Map<string, GeneratorInfo> {
        return new Map(this.generators);
    }

    

    static getByCategory(category: GeneratorMetadata['category']): GeneratorInfo[] {
        return Array.from(this.generators.values())
            .filter(info => info.metadata.category === category);
    }

    

    static get(name: string): GeneratorInfo | undefined {
        return this.generators.get(name);
    }

    

    static isAvailable(name: string): boolean {
        const info = this.generators.get(name);
        return info !== undefined && info.isEnabled;
    }

    

    static setEnabled(name: string, enabled: boolean): void {
        const info = this.generators.get(name);
        if (info) {
            info.isEnabled = enabled;
        }
    }

    

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

    integrationGenerator: IntegrationGenerator;
    configurationGenerator: ConfigurationGenerator;

    sagaGenerator: SagaGenerator;
    sagaFunctionalityGenerator: SagaFunctionalityGenerator;

    exceptionGenerator: ExceptionGenerator;
    eventHandlerGenerator: EventHandlerGenerator;

    validationSystem: AggregateValidator;
}

export class GeneratorRegistryFactory {
    private static initialized = false;

    

    private static initializeGenerators(): void {
        if (this.initialized) return;

        
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

        
        GeneratorDiscovery.register('validationSystem', new AggregateValidator(), {
            name: 'Validation System',
            version: '1.0.0',
            description: 'Comprehensive validation system',
            category: 'validation',
            dependencies: [],
            outputTypes: ['java'],
            isRequired: false
        });

        
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

    

    static createRegistry(): GeneratorRegistry {
        this.initializeGenerators();

        
        const validation = GeneratorDiscovery.validateDependencies();
        if (!validation.valid) {
            console.warn('Generator dependency validation failed:');
            validation.errors.forEach(error => console.warn(`  - ${error}`));
        }

        
        const registry: any = {};

        for (const [name, info] of GeneratorDiscovery.getAll()) {
            if (info.isEnabled) {
                registry[name] = info.instance;
            }
        }

        return registry as GeneratorRegistry;
    }

    

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

    

    static getGenerationOrder(): string[] {
        this.initializeGenerators();
        return GeneratorDiscovery.getGenerationOrder();
    }

    

    static configureByCategory(category: GeneratorMetadata['category'], enabled: boolean): void {
        this.initializeGenerators();

        const generators = GeneratorDiscovery.getByCategory(category);
        generators.forEach(info => {
            GeneratorDiscovery.setEnabled(info.metadata.name, enabled);
        });
    }

    

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
