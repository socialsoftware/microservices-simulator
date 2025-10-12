import { EntityGenerator } from "../generator/impl/microservices/entity/entity-generator.js";
import { DtoGenerator } from "../generator/impl/microservices/entity/dto-generator.js";
import { ServiceGenerator } from "../generator/impl/microservices/entity/service-generator.js";
import { ServiceDefinitionGenerator } from "../generator/impl/microservices/service/service-definition-generator.js";
import { FactoryGenerator } from "../generator/impl/microservices/entity/factory-generator.js";
import { RepositoryGenerator } from "../generator/impl/microservices/repository/repository-generator.js";
import { RepositoryInterfaceGenerator } from "../generator/impl/microservices/repository/repository-interface-generator.js";
import { EventGenerator } from "../generator/impl/microservices/events/event-generator.js";
import { CoordinationGenerator } from "../generator/impl/coordination/index.js";
import { WebApiGenerator } from "../generator/impl/microservices/web/webapi-generator.js";
import { ValidationGenerator } from "../generator/validation/validation-generator.js";
import { IntegrationGenerator } from "../generator/impl/coordination/config/integration-generator.js";
import { SagaGenerator } from "../generator/impl/sagas/saga-generator.js";
import { SagaFunctionalityGenerator } from "../generator/impl/sagas/saga-functionality-generator.js";
import { TestGenerator } from "../generator/impl/microservices/testing/test-generator.js";
import { ExceptionGenerator } from "../generator/impl/microservices/web/exception-generator.js";
import { EventHandlerGenerator } from "../generator/impl/microservices/events/event-handler-generator.js";
import { CausalEntityGenerator } from "../generator/impl/sagas/causal-entity-generator.js";
import { ConfigurationGenerator } from "../generator/impl/coordination/config/configuration-generator.js";
import { ValidationSystem } from "../generator/validation/validation-system.js";

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
