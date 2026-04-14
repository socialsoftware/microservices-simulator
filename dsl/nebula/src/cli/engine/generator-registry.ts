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

const GENERATOR_FACTORIES: { [K in keyof GeneratorRegistry]: () => GeneratorRegistry[K] } = {
    entityGenerator:              () => new EntityGenerator(),
    dtoGenerator:                 () => new DtoGenerator(),
    serviceGenerator:             () => new ServiceGenerator(),
    serviceDefinitionGenerator:   () => new ServiceDefinitionGenerator(),
    factoryGenerator:             () => new FactoryGenerator(),
    repositoryGenerator:          () => new RepositoryGenerator(),
    repositoryInterfaceGenerator: () => new RepositoryInterfaceGenerator(),
    eventGenerator:               () => new EventGenerator(),
    eventHandlerGenerator:        () => new EventHandlerGenerator(),
    exceptionGenerator:           () => new ExceptionGenerator(),
    coordinationGenerator:        () => new CoordinationGenerator(),
    webApiGenerator:              () => new WebApiGenerator(),
    integrationGenerator:         () => new IntegrationGenerator(),
    configurationGenerator:       () => new ConfigurationGenerator(),
    sagaGenerator:                () => new SagaGenerator(),
    sagaFunctionalityGenerator:   () => new SagaFunctionalityGenerator(),
    validationSystem:             () => new AggregateValidator(),
};

export class GeneratorRegistryFactory {
    private static cached: GeneratorRegistry | null = null;

    static createRegistry(): GeneratorRegistry {
        if (this.cached) return this.cached;

        const registry = {} as GeneratorRegistry;
        for (const key of Object.keys(GENERATOR_FACTORIES) as Array<keyof GeneratorRegistry>) {
            (registry[key] as any) = GENERATOR_FACTORIES[key]();
        }
        this.cached = registry;
        return registry;
    }
}
