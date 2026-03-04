


import { getGlobalConfig } from "../generators/common/config.js";



export enum PackageType {
    MICROSERVICES = 'microservices',
    COORDINATION = 'coordination',
    SAGAS = 'sagas',
    SHARED = 'shared',
    VALIDATION = 'validation',
    EVENTS = 'events',
    CONFIG = 'config'
}



export enum ComponentType {
    
    AGGREGATE = 'aggregate',
    SERVICE = 'service',
    REPOSITORY = 'repository',
    EVENTS_PUBLISH = 'events.publish',
    EVENTS_SUBSCRIBE = 'events.subscribe',
    EVENTS_HANDLING = 'events.handling',
    EVENTS_HANDLERS = 'events.handling.handlers',

    
    FUNCTIONALITIES = 'functionalities',
    EVENT_PROCESSING = 'eventProcessing',
    WEBAPI = 'webapi',
    WEBAPI_DTOS = 'webapi.dtos',

    
    AGGREGATES = 'aggregates',
    STATES = 'aggregates.states',
    DTOS = 'aggregates.dtos',
    FACTORIES = 'aggregates.factories',
    REPOSITORIES = 'aggregates.repositories',
    COORDINATION_SAGA = 'coordination',

    
    DTOS_SHARED = 'dtos',
    ENUMS = 'enums',
    EXCEPTIONS = 'exceptions',
    UTILS = 'utils',

    
    INVARIANTS = 'invariants',
    ANNOTATIONS = 'annotations',
    VALIDATORS = 'validators'
}



export interface PackageBuildingOptions {
    projectName: string;
    basePackage?: string;
    aggregateName?: string;
    customSegments?: string[];
    lowercase?: boolean;
}



export class PackageNameBuilder {
    private basePackage: string;

    constructor(basePackage?: string) {
        this.basePackage = basePackage || getGlobalConfig().getBasePackage();
    }

    

    buildMicroservicePackage(
        projectName: string,
        aggregateName: string,
        component: ComponentType | string
    ): string {
        const segments = [
            this.basePackage,
            projectName.toLowerCase(),
            PackageType.MICROSERVICES,
            aggregateName.toLowerCase(),
            component
        ];

        return segments.join('.');
    }

    

    buildCoordinationPackage(
        projectName: string,
        component: ComponentType | string,
        aggregateName?: string
    ): string {
        if (aggregateName) {
            const segments = [
                this.basePackage,
                projectName.toLowerCase(),
                PackageType.MICROSERVICES,
                aggregateName.toLowerCase(),
                PackageType.COORDINATION,
                component
            ];
            return segments.join('.');
        }
        const segments = [
            this.basePackage,
            projectName.toLowerCase(),
            PackageType.COORDINATION,
            component
        ];

        return segments.join('.');
    }

    

    buildSagaPackage(
        projectName: string,
        component: ComponentType | string,
        aggregateName?: string
    ): string {
        if (aggregateName) {
            if (component === ComponentType.COORDINATION_SAGA) {
                return [
                    this.basePackage,
                    projectName.toLowerCase(),
                    PackageType.MICROSERVICES,
                    aggregateName.toLowerCase(),
                    PackageType.COORDINATION,
                    'sagas'
                ].join('.');
            }
            return [
                this.basePackage,
                projectName.toLowerCase(),
                PackageType.MICROSERVICES,
                aggregateName.toLowerCase(),
                'aggregate',
                'sagas',
                ...(component === ComponentType.AGGREGATES ? [] : [component.replace('aggregates.', '')])
            ].join('.');
        }
        const segments = [
            this.basePackage,
            projectName.toLowerCase(),
            PackageType.SAGAS
        ];

        if (component === ComponentType.COORDINATION_SAGA && aggregateName) {
            segments.push(ComponentType.COORDINATION_SAGA, aggregateName.toLowerCase());
        } else {
            segments.push(component);
        }

        return segments.join('.');
    }

    

    buildSharedPackage(
        projectName: string,
        component: ComponentType | string
    ): string {
        const segments = [
            this.basePackage,
            projectName.toLowerCase(),
            PackageType.SHARED,
            component
        ];

        return segments.join('.');
    }

    

    buildValidationPackage(
        projectName: string,
        aggregateName: string,
        component: ComponentType | string
    ): string {
        const segments = [
            this.basePackage,
            projectName.toLowerCase(),
            PackageType.COORDINATION,
            PackageType.VALIDATION,
            component
        ];

        return segments.join('.');
    }

    

    buildCustomPackage(projectName: string, ...segments: string[]): string {
        const allSegments = [
            this.basePackage,
            projectName.toLowerCase(),
            ...segments
        ];

        return allSegments.join('.');
    }

    

    buildFrameworkPackage(...segments: string[]): string {
        return [this.basePackage, ...segments].join('.');
    }

    

    getBasePackage(): string {
        return this.basePackage;
    }

    


    
    buildEntityPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.AGGREGATE);
    }

    buildServicePackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.SERVICE);
    }

    buildRepositoryPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.REPOSITORY);
    }

    
    buildPublishedEventPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.EVENTS_PUBLISH);
    }

    buildSubscribedEventPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.EVENTS_SUBSCRIBE);
    }

    buildEventHandlingPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.EVENTS_HANDLING);
    }

    buildEventHandlersPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.EVENTS_HANDLERS);
    }

    
    buildFunctionalitiesPackage(projectName: string, aggregateName?: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.FUNCTIONALITIES, aggregateName);
    }

    buildEventProcessingPackage(projectName: string, aggregateName?: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.EVENT_PROCESSING, aggregateName);
    }

    buildWebApiPackage(projectName: string, aggregateName?: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.WEBAPI, aggregateName);
    }

    buildWebApiDtosPackage(projectName: string, aggregateName?: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.WEBAPI_DTOS, aggregateName);
    }


    buildSagaAggregatesPackage(projectName: string, aggregateName?: string): string {
        return this.buildSagaPackage(projectName, ComponentType.AGGREGATES, aggregateName);
    }

    buildSagaStatesPackage(projectName: string, aggregateName?: string): string {
        return this.buildSagaPackage(projectName, ComponentType.STATES, aggregateName);
    }

    buildSagaDtosPackage(projectName: string, aggregateName?: string): string {
        return this.buildSagaPackage(projectName, ComponentType.DTOS, aggregateName);
    }

    buildSagaFactoriesPackage(projectName: string, aggregateName?: string): string {
        return this.buildSagaPackage(projectName, ComponentType.FACTORIES, aggregateName);
    }

    buildSagaRepositoriesPackage(projectName: string, aggregateName?: string): string {
        return this.buildSagaPackage(projectName, ComponentType.REPOSITORIES, aggregateName);
    }

    buildSagaCoordinationPackage(projectName: string, aggregateName: string): string {
        return this.buildSagaPackage(projectName, ComponentType.COORDINATION_SAGA, aggregateName);
    }

    buildEventsPackage(projectName: string): string {
        return [this.basePackage, projectName.toLowerCase(), 'events'].join('.');
    }

    buildCommandPackage(projectName: string, aggregateName: string): string {
        return [this.basePackage, projectName.toLowerCase(), 'command', aggregateName.toLowerCase()].join('.');
    }

    buildCommandHandlerPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, 'commandHandler');
    }

    buildCausalPackage(projectName: string, aggregateName: string): string {
        return [
            this.basePackage,
            projectName.toLowerCase(),
            PackageType.MICROSERVICES,
            aggregateName.toLowerCase(),
            'aggregate',
            'causal'
        ].join('.');
    }

    
    buildSharedDtosPackage(projectName: string): string {
        return this.buildSharedPackage(projectName, ComponentType.DTOS_SHARED);
    }

    buildSharedEnumsPackage(projectName: string): string {
        return this.buildSharedPackage(projectName, ComponentType.ENUMS);
    }

    buildExceptionsPackage(projectName: string): string {
        return this.buildSharedPackage(projectName, ComponentType.EXCEPTIONS);
    }

    
    buildInvariantsPackage(projectName: string, aggregateName: string): string {
        return this.buildValidationPackage(projectName, aggregateName, ComponentType.INVARIANTS);
    }

    buildAnnotationsPackage(projectName: string, aggregateName: string): string {
        return this.buildValidationPackage(projectName, aggregateName, ComponentType.ANNOTATIONS);
    }

    buildValidatorsPackage(projectName: string, aggregateName: string): string {
        return this.buildValidationPackage(projectName, aggregateName, ComponentType.VALIDATORS);
    }

    
    buildTransactionModelPackage(): string {
        return this.buildFrameworkPackage('ms', 'TransactionalModel');
    }

    buildUnitOfWorkPackage(): string {
        return this.buildFrameworkPackage('ms', 'coordination', 'unitOfWork');
    }

    buildSagaFrameworkPackage(): string {
        return this.buildFrameworkPackage('ms', 'sagas');
    }

    buildDomainPackage(): string {
        return this.buildFrameworkPackage('ms', 'domain');
    }
}



export class PackageBuilderFactory {
    

    static createStandard(): PackageNameBuilder {
        return new PackageNameBuilder();
    }

    

    static createWithBasePackage(basePackage: string): PackageNameBuilder {
        return new PackageNameBuilder(basePackage);
    }

    

    static createForTesting(): PackageNameBuilder {
        return new PackageNameBuilder('com.test.example');
    }
}



export class PackageUtils {
    

    static extractProjectName(packageName: string): string | null {
        const parts = packageName.split('.');
        const basePackage = getGlobalConfig().getBasePackage();
        const baseParts = basePackage.split('.');

        if (parts.length > baseParts.length) {
            return parts[baseParts.length];
        }

        return null;
    }

    

    static extractAggregateName(packageName: string): string | null {
        const parts = packageName.split('.');
        const microservicesIndex = parts.indexOf('microservices');

        if (microservicesIndex !== -1 && parts.length > microservicesIndex + 1) {
            return parts[microservicesIndex + 1];
        }

        return null;
    }

    

    static isPackageType(packageName: string, type: PackageType): boolean {
        return packageName.includes(`.${type}.`);
    }

    

    static classNameToComponent(className: string): string {
        if (className.endsWith('Service')) return ComponentType.SERVICE;
        if (className.endsWith('Repository')) return ComponentType.REPOSITORY;
        if (className.endsWith('Controller')) return ComponentType.WEBAPI;
        if (className.endsWith('Dto')) return ComponentType.DTOS_SHARED;
        if (className.endsWith('Event')) return ComponentType.EVENTS_PUBLISH;
        if (className.endsWith('Handler')) return ComponentType.EVENTS_HANDLERS;
        if (className.endsWith('Factory')) return ComponentType.FACTORIES;
        if (className.endsWith('Exception')) return ComponentType.EXCEPTIONS;

        return ComponentType.AGGREGATE; 
    }

    

    static validatePackageName(packageName: string): string[] {
        const errors: string[] = [];

        if (!packageName) {
            errors.push('Package name cannot be empty');
            return errors;
        }

        if (!packageName.match(/^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$/)) {
            errors.push('Package name must follow Java package naming conventions');
        }

        const basePackage = getGlobalConfig().getBasePackage();
        if (!packageName.startsWith(basePackage)) {
            errors.push(`Package name must start with base package: ${basePackage}`);
        }

        return errors;
    }
}
