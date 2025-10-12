import { EntityGenerator } from "../generators/microservices/entity/entity-generator.js";
import { DtoGenerator } from "../generators/microservices/entity/dto-generator.js";
import { ServiceGenerator } from "../generators/microservices/entity/service-generator.js";
import { ServiceDefinitionGenerator } from "../generators/microservices/service/service-definition-generator.js";
import { FactoryGenerator } from "../generators/microservices/entity/factory-generator.js";
import { RepositoryGenerator } from "../generators/microservices/repository/repository-generator.js";
import { RepositoryInterfaceGenerator } from "../generators/microservices/repository/repository-interface-generator.js";
import { EventGenerator } from "../generators/microservices/events/event-generator.js";
import { CoordinationGenerator } from "../generators/coordination/index.js";
import { WebApiGenerator } from "../generators/microservices/web/webapi-generator.js";
import { ValidationGenerator } from "../generators/validation/validation-generator.js";
import { IntegrationGenerator } from "../generators/coordination/config/integration-generator.js";
import { SagaGenerator } from "../generators/sagas/saga-generator.js";
import { SagaFunctionalityGenerator } from "../generators/sagas/saga-functionality-generator.js";
import { TestGenerator } from "../generators/microservices/testing/test-generator.js";
import { ExceptionGenerator } from "../generators/microservices/web/exception-generator.js";
import { EventHandlerGenerator } from "../generators/microservices/events/event-handler-generator.js";
import { CausalEntityGenerator } from "../generators/sagas/causal-entity-generator.js";
import { ConfigurationGenerator } from "../generators/coordination/config/configuration-generator.js";
import { ValidationSystem } from "../generators/validation/validation-system.js";

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

    testGenerator: TestGenerator;
    exceptionGenerator: ExceptionGenerator;
    eventHandlerGenerator: EventHandlerGenerator;

    validationSystem: ValidationSystem;
}

export class GeneratorRegistryFactory {
    static createRegistry(): GeneratorRegistry {
        return {
            entityGenerator: new EntityGenerator(),
            dtoGenerator: new DtoGenerator(),
            serviceGenerator: new ServiceGenerator(),
            serviceDefinitionGenerator: new ServiceDefinitionGenerator(),
            factoryGenerator: new FactoryGenerator(),
            repositoryGenerator: new RepositoryGenerator(),
            repositoryInterfaceGenerator: new RepositoryInterfaceGenerator(),

            eventGenerator: new EventGenerator(),
            coordinationGenerator: new CoordinationGenerator(),
            webApiGenerator: new WebApiGenerator(),
            validationGenerator: new ValidationGenerator(),

            integrationGenerator: new IntegrationGenerator(),
            configurationGenerator: new ConfigurationGenerator(),

            sagaGenerator: new SagaGenerator(),
            sagaFunctionalityGenerator: new SagaFunctionalityGenerator(),
            causalEntityGenerator: new CausalEntityGenerator(),

            testGenerator: new TestGenerator(),
            exceptionGenerator: new ExceptionGenerator(),
            eventHandlerGenerator: new EventHandlerGenerator(),

            validationSystem: new ValidationSystem()
        };
    }
}
