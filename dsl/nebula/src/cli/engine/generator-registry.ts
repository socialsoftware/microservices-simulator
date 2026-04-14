import { EntityGenerator } from "../generators/microservices/entity/entity-orchestrator.js";
import { DtoGenerator } from "../generators/microservices/shared/dto-generator.js";
import { ServiceGenerator } from "../generators/microservices/service/default/main.js";
import { ServiceDefinitionGenerator } from "../generators/microservices/service/service-definition-generator.js";
import { FactoryGenerator } from "../generators/microservices/factory/factory-generator.js";
import { RepositoryGenerator } from "../generators/microservices/repository/repository-generator.js";
import { RepositoryInterfaceGenerator } from "../generators/microservices/repository/repository-interface-generator.js";
import { EventGenerator } from "../generators/microservices/events/event-orchestrator.js";
import { CoordinationGenerator } from "../generators/coordination/index.js";
import { WebApiGenerator } from "../generators/coordination/webapi/webapi-generator.js";
import { IntegrationGenerator } from "../generators/coordination/config/integration-generator.js";
import { SagaGenerator } from "../generators/sagas/saga-generator.js";
import { SagaFunctionalityGenerator } from "../generators/sagas/saga-functionality-generator.js";
import { ExceptionGenerator } from "../generators/common/exception-generator.js";
import { EventHandlerGenerator } from "../generators/microservices/events/event-handler-generator.js";
import { ConfigurationGenerator } from "../generators/coordination/config/configuration-generator.js";
import { AggregateValidator } from "../generators/validation/validation-system.js";

type GeneratorCategory = 'microservices' | 'coordination' | 'validation' | 'sagas' | 'config';

export interface GeneratorMetadata {
    name: string;
    version: string;
    description: string;
    category: GeneratorCategory;
    dependencies: string[];
    outputTypes: string[];
    isRequired: boolean;
}

export interface GeneratorInfo {
    instance: any;
    metadata: GeneratorMetadata;
    isEnabled: boolean;
}

interface GeneratorDefinition {
    key: string;
    factory: () => any;
    category: GeneratorCategory;
    dependencies: string[];
    required: boolean;
    description: string;
}

const GENERATOR_DEFINITIONS: GeneratorDefinition[] = [
    { key: 'entityGenerator',              factory: () => new EntityGenerator(),              category: 'microservices',  dependencies: [],                                required: true,  description: 'Generates JPA entities from DSL definitions' },
    { key: 'dtoGenerator',                 factory: () => new DtoGenerator(),                 category: 'microservices',  dependencies: ['entityGenerator'],               required: true,  description: 'Generates Data Transfer Objects' },
    { key: 'serviceGenerator',             factory: () => new ServiceGenerator(),             category: 'microservices',  dependencies: ['entityGenerator', 'dtoGenerator'], required: true, description: 'Generates service classes with business logic' },
    { key: 'serviceDefinitionGenerator',   factory: () => new ServiceDefinitionGenerator(),   category: 'microservices',  dependencies: ['entityGenerator'],               required: false, description: 'Generates service definitions from DSL' },
    { key: 'factoryGenerator',             factory: () => new FactoryGenerator(),             category: 'microservices',  dependencies: ['entityGenerator'],               required: true,  description: 'Generates factory classes for entity creation' },
    { key: 'repositoryGenerator',          factory: () => new RepositoryGenerator(),          category: 'microservices',  dependencies: ['entityGenerator'],               required: true,  description: 'Generates JPA repository implementations' },
    { key: 'repositoryInterfaceGenerator', factory: () => new RepositoryInterfaceGenerator(), category: 'microservices',  dependencies: ['entityGenerator'],               required: true,  description: 'Generates repository interfaces' },
    { key: 'eventGenerator',               factory: () => new EventGenerator(),               category: 'microservices',  dependencies: ['entityGenerator'],               required: false, description: 'Generates domain events' },
    { key: 'eventHandlerGenerator',        factory: () => new EventHandlerGenerator(),        category: 'microservices',  dependencies: ['eventGenerator'],                required: false, description: 'Generates event handler implementations' },
    { key: 'exceptionGenerator',           factory: () => new ExceptionGenerator(),           category: 'microservices',  dependencies: [],                                required: false, description: 'Generates custom exception classes' },
    { key: 'coordinationGenerator',        factory: () => new CoordinationGenerator(),        category: 'coordination',   dependencies: ['serviceGenerator'],              required: false, description: 'Generates coordination layer components' },
    { key: 'webApiGenerator',              factory: () => new WebApiGenerator(),              category: 'coordination',   dependencies: ['serviceGenerator', 'dtoGenerator'], required: false, description: 'Generates REST API controllers' },
    { key: 'integrationGenerator',         factory: () => new IntegrationGenerator(),         category: 'config',         dependencies: [],                                required: false, description: 'Generates integration configurations' },
    { key: 'configurationGenerator',       factory: () => new ConfigurationGenerator(),       category: 'config',         dependencies: [],                                required: false, description: 'Generates application configurations' },
    { key: 'sagaGenerator',                factory: () => new SagaGenerator(),                category: 'sagas',          dependencies: ['entityGenerator', 'serviceGenerator'], required: false, description: 'Generates saga orchestration logic' },
    { key: 'sagaFunctionalityGenerator',   factory: () => new SagaFunctionalityGenerator(),   category: 'sagas',          dependencies: ['sagaGenerator'],                 required: false, description: 'Generates saga functionality implementations' },
    { key: 'validationSystem',             factory: () => new AggregateValidator(),           category: 'validation',     dependencies: [],                                required: false, description: 'Comprehensive validation system' },
];

export class GeneratorDiscovery {
    private static generators = new Map<string, GeneratorInfo>();

    static register(name: string, instance: any, metadata: GeneratorMetadata): void {
        this.generators.set(name, { instance, metadata, isEnabled: true });
    }

    static getAll(): Map<string, GeneratorInfo> {
        return new Map(this.generators);
    }

    static getByCategory(category: GeneratorCategory): GeneratorInfo[] {
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
        if (info) info.isEnabled = enabled;
    }

    static validateDependencies(): { valid: boolean; errors: string[] } {
        const errors: string[] = [];
        for (const [name, info] of this.generators) {
            if (!info.isEnabled) continue;
            for (const dep of info.metadata.dependencies) {
                if (!this.isAvailable(dep)) {
                    errors.push(`Generator '${name}' requires '${dep}' but it's not available`);
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
            for (const dep of info.metadata.dependencies) visit(dep);
            visiting.delete(name);
            visited.add(name);
            order.push(name);
        };

        for (const name of this.generators.keys()) visit(name);
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

        for (const def of GENERATOR_DEFINITIONS) {
            GeneratorDiscovery.register(def.key, def.factory(), {
                name: def.key,
                version: '1.0.0',
                description: def.description,
                category: def.category,
                dependencies: def.dependencies,
                outputTypes: ['java'],
                isRequired: def.required,
            });
        }

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
            if (info.isEnabled) registry[name] = info.instance;
        }
        return registry as GeneratorRegistry;
    }

    static createMinimalRegistry(): Partial<GeneratorRegistry> {
        this.initializeGenerators();

        const registry: any = {};
        for (const [name, info] of GeneratorDiscovery.getAll()) {
            if (info.isEnabled && info.metadata.isRequired) registry[name] = info.instance;
        }
        return registry;
    }
}
