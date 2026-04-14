import * as path from "node:path";

export class AggregatePaths {
    readonly aggregatePath: string;
    readonly aggregateName: string;

    constructor(aggregatePath: string, aggregateName: string) {
        this.aggregatePath = aggregatePath;
        this.aggregateName = aggregateName;
    }

    entity(name: string): string { return this.agg(`${name}.java`); }
    factory(): string { return this.agg(`${this.aggregateName}Factory.java`); }
    repositoryInterface(): string { return this.agg(`${this.aggregateName}Repository.java`); }
    customRepository(): string { return this.agg(`${this.aggregateName}CustomRepository.java`); }
    aggregateBaseClass(): string { return this.agg(`${this.aggregateName}.java`); }

    service(fileName?: string): string { return this.svc(fileName ?? `${this.aggregateName}Service.java`); }
    serviceExtension(fileName: string): string { return this.svc(fileName); }

    eventHandling(): string { return this.evt('handling', `${this.aggregateName}EventHandling.java`); }
    eventBaseHandler(): string { return this.evt('handling', 'handlers', `${this.aggregateName}EventHandler.java`); }
    eventHandler(handlerName: string): string { return this.evt('handling', 'handlers', `${handlerName}.java`); }
    eventSubscription(name: string): string { return this.evt('subscribe', `${name}.java`); }

    functionalities(): string { return this.coord('functionalities', `${this.aggregateName}Functionalities.java`); }
    eventProcessing(): string { return this.coord('eventProcessing', `${this.aggregateName}EventProcessing.java`); }
    controller(): string { return this.coord('webapi', `${this.aggregateName}Controller.java`); }
    requestDtosDir(): string { return this.coord('webapi', 'requestDtos'); }
    requestDto(name: string): string { return this.coord('webapi', 'requestDtos', `${name}.java`); }
    sagaFunctionality(fileName: string): string { return this.coord('sagas', fileName); }

    sagaAggregate(): string { return this.agg('sagas', `Saga${this.aggregateName}.java`); }
    sagaDto(): string { return this.agg('sagas', 'dtos', `Saga${this.aggregateName}Dto.java`); }
    sagaState(): string { return this.agg('sagas', 'states', `${this.aggregateName}SagaState.java`); }
    sagaFactory(): string { return this.agg('sagas', 'factories', `Sagas${this.aggregateName}Factory.java`); }
    sagaRepository(): string { return this.agg('sagas', 'repositories', `${this.aggregateName}CustomRepositorySagas.java`); }

    sharedDto(entityName: string): string { return this.projectRoot('shared', 'dtos', `${entityName}Dto.java`); }
    sharedEventsDir(): string { return this.projectRoot('events'); }
    sharedEvent(fileName: string): string { return this.projectRoot('events', `${fileName}.java`); }

    private agg(...parts: string[]): string { return path.join(this.aggregatePath, 'aggregate', ...parts); }
    private svc(...parts: string[]): string { return path.join(this.aggregatePath, 'service', ...parts); }
    private evt(...parts: string[]): string { return path.join(this.aggregatePath, 'events', ...parts); }
    private coord(...parts: string[]): string { return path.join(this.aggregatePath, 'coordination', ...parts); }
    private projectRoot(...parts: string[]): string {
        return path.join(this.aggregatePath, '..', '..', ...parts);
    }
}

export class ProjectPaths {
    constructor(readonly javaPath: string) { }

    behaviourController(): string { return path.join(this.javaPath, 'coordination', 'webapi', 'BehaviourController.java'); }
    tracesController(): string { return path.join(this.javaPath, 'coordination', 'webapi', 'TracesController.java'); }

    aggregateDir(aggregateName: string): string {
        return path.join(this.javaPath, 'microservices', aggregateName.toLowerCase());
    }
}
