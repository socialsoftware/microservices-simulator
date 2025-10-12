/**
 * Centralized Package Name Building System
 * 
 * This module provides comprehensive package name generation with consistent
 * patterns across all generators, eliminating the 99+ instances of duplicate
 * package building logic found throughout the codebase.
 */

import { getGlobalConfig } from "../generators/common/config.js";

/**
 * Package types for different components of the system
 */
export enum PackageType {
    MICROSERVICES = 'microservices',
    COORDINATION = 'coordination',
    SAGAS = 'sagas',
    SHARED = 'shared',
    VALIDATION = 'validation',
    EVENTS = 'events',
    CONFIG = 'config'
}

/**
 * Component types within each package type
 */
export enum ComponentType {
    // Microservice components
    AGGREGATE = 'aggregate',
    SERVICE = 'service',
    REPOSITORY = 'repository',
    EVENTS_PUBLISH = 'events.publish',
    EVENTS_SUBSCRIBE = 'events.subscribe',
    EVENTS_HANDLING = 'events.handling',
    EVENTS_HANDLERS = 'events.handling.handlers',

    // Coordination components
    FUNCTIONALITIES = 'functionalities',
    EVENT_PROCESSING = 'eventProcessing',
    WEBAPI = 'webapi',
    WEBAPI_DTOS = 'webapi.dtos',

    // Saga components
    AGGREGATES = 'aggregates',
    STATES = 'aggregates.states',
    DTOS = 'aggregates.dtos',
    FACTORIES = 'aggregates.factories',
    REPOSITORIES = 'aggregates.repositories',
    COORDINATION_SAGA = 'coordination',

    // Shared components
    DTOS_SHARED = 'dtos',
    ENUMS = 'enums',
    EXCEPTIONS = 'exceptions',
    UTILS = 'utils',

    // Validation components
    INVARIANTS = 'invariants',
    ANNOTATIONS = 'annotations',
    VALIDATORS = 'validators'
}

/**
 * Package building options
 */
export interface PackageBuildingOptions {
    projectName: string;
    basePackage?: string;
    aggregateName?: string;
    customSegments?: string[];
    lowercase?: boolean;
}

/**
 * Centralized package name builder with consistent patterns
 */
export class PackageNameBuilder {
    private basePackage: string;

    constructor(basePackage?: string) {
        this.basePackage = basePackage || getGlobalConfig().getBasePackage();
    }

    /**
     * Build a microservice package name
     */
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

    /**
     * Build a coordination package name
     */
    buildCoordinationPackage(
        projectName: string,
        component: ComponentType | string
    ): string {
        const segments = [
            this.basePackage,
            projectName.toLowerCase(),
            PackageType.COORDINATION,
            component
        ];

        return segments.join('.');
    }

    /**
     * Build a saga package name
     */
    buildSagaPackage(
        projectName: string,
        component: ComponentType | string,
        aggregateName?: string
    ): string {
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

    /**
     * Build a shared package name
     */
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

    /**
     * Build a validation package name
     */
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

    /**
     * Build a custom package name with arbitrary segments
     */
    buildCustomPackage(projectName: string, ...segments: string[]): string {
        const allSegments = [
            this.basePackage,
            projectName.toLowerCase(),
            ...segments
        ];

        return allSegments.join('.');
    }

    /**
     * Build framework/base package names (for framework imports)
     */
    buildFrameworkPackage(...segments: string[]): string {
        return [this.basePackage, ...segments].join('.');
    }

    /**
     * Get the base package
     */
    getBasePackage(): string {
        return this.basePackage;
    }

    /**
     * Specialized builders for common patterns
     */

    // Entity-related packages
    buildEntityPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.AGGREGATE);
    }

    buildServicePackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.SERVICE);
    }

    buildRepositoryPackage(projectName: string, aggregateName: string): string {
        return this.buildMicroservicePackage(projectName, aggregateName, ComponentType.REPOSITORY);
    }

    // Event-related packages
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

    // Coordination-related packages
    buildFunctionalitiesPackage(projectName: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.FUNCTIONALITIES);
    }

    buildEventProcessingPackage(projectName: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.EVENT_PROCESSING);
    }

    buildWebApiPackage(projectName: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.WEBAPI);
    }

    buildWebApiDtosPackage(projectName: string): string {
        return this.buildCoordinationPackage(projectName, ComponentType.WEBAPI_DTOS);
    }

    // Saga-related packages
    buildSagaAggregatesPackage(projectName: string): string {
        return this.buildSagaPackage(projectName, ComponentType.AGGREGATES);
    }

    buildSagaStatesPackage(projectName: string): string {
        return this.buildSagaPackage(projectName, ComponentType.STATES);
    }

    buildSagaDtosPackage(projectName: string): string {
        return this.buildSagaPackage(projectName, ComponentType.DTOS);
    }

    buildSagaFactoriesPackage(projectName: string): string {
        return this.buildSagaPackage(projectName, ComponentType.FACTORIES);
    }

    buildSagaRepositoriesPackage(projectName: string): string {
        return this.buildSagaPackage(projectName, ComponentType.REPOSITORIES);
    }

    buildSagaCoordinationPackage(projectName: string, aggregateName: string): string {
        return this.buildSagaPackage(projectName, ComponentType.COORDINATION_SAGA, aggregateName);
    }

    // Shared packages
    buildSharedDtosPackage(projectName: string): string {
        return this.buildSharedPackage(projectName, ComponentType.DTOS_SHARED);
    }

    buildSharedEnumsPackage(projectName: string): string {
        return this.buildSharedPackage(projectName, ComponentType.ENUMS);
    }

    buildExceptionsPackage(projectName: string): string {
        return this.buildSharedPackage(projectName, ComponentType.EXCEPTIONS);
    }

    // Validation packages
    buildInvariantsPackage(projectName: string, aggregateName: string): string {
        return this.buildValidationPackage(projectName, aggregateName, ComponentType.INVARIANTS);
    }

    buildAnnotationsPackage(projectName: string, aggregateName: string): string {
        return this.buildValidationPackage(projectName, aggregateName, ComponentType.ANNOTATIONS);
    }

    buildValidatorsPackage(projectName: string, aggregateName: string): string {
        return this.buildValidationPackage(projectName, aggregateName, ComponentType.VALIDATORS);
    }

    // Framework packages (for imports)
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

/**
 * Factory for creating package builders with different configurations
 */
export class PackageBuilderFactory {
    /**
     * Create a standard package builder using global config
     */
    static createStandard(): PackageNameBuilder {
        return new PackageNameBuilder();
    }

    /**
     * Create a package builder with custom base package
     */
    static createWithBasePackage(basePackage: string): PackageNameBuilder {
        return new PackageNameBuilder(basePackage);
    }

    /**
     * Create a package builder for testing (with test base package)
     */
    static createForTesting(): PackageNameBuilder {
        return new PackageNameBuilder('com.test.example');
    }
}

/**
 * Utility functions for package name operations
 */
export class PackageUtils {
    /**
     * Extract project name from a full package name
     */
    static extractProjectName(packageName: string): string | null {
        const parts = packageName.split('.');
        const basePackage = getGlobalConfig().getBasePackage();
        const baseParts = basePackage.split('.');

        if (parts.length > baseParts.length) {
            return parts[baseParts.length];
        }

        return null;
    }

    /**
     * Extract aggregate name from a microservice package name
     */
    static extractAggregateName(packageName: string): string | null {
        const parts = packageName.split('.');
        const microservicesIndex = parts.indexOf('microservices');

        if (microservicesIndex !== -1 && parts.length > microservicesIndex + 1) {
            return parts[microservicesIndex + 1];
        }

        return null;
    }

    /**
     * Check if a package name belongs to a specific type
     */
    static isPackageType(packageName: string, type: PackageType): boolean {
        return packageName.includes(`.${type}.`);
    }

    /**
     * Convert a class name to its likely package component
     */
    static classNameToComponent(className: string): string {
        if (className.endsWith('Service')) return ComponentType.SERVICE;
        if (className.endsWith('Repository')) return ComponentType.REPOSITORY;
        if (className.endsWith('Controller')) return ComponentType.WEBAPI;
        if (className.endsWith('Dto')) return ComponentType.DTOS_SHARED;
        if (className.endsWith('Event')) return ComponentType.EVENTS_PUBLISH;
        if (className.endsWith('Handler')) return ComponentType.EVENTS_HANDLERS;
        if (className.endsWith('Factory')) return ComponentType.FACTORIES;
        if (className.endsWith('Exception')) return ComponentType.EXCEPTIONS;

        return ComponentType.AGGREGATE; // Default
    }

    /**
     * Validate package name format
     */
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
