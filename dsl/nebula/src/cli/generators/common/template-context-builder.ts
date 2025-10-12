/**
 * Unified Template Context Building System
 * 
 * This module provides a comprehensive, fluent API for building template contexts
 * across all generators, eliminating duplicate context building patterns and
 * ensuring consistency in template data structure.
 */

import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "./unified-type-resolver.js";
import { PackageNameBuilder, PackageBuilderFactory } from "../../utils/package-name-builder.js";
import { getGlobalConfig } from "./config.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../utils/error-handler.js";

/**
 * Standard template context interface
 */
export interface TemplateContext {
    // Project information
    projectName?: string;
    ProjectName?: string; // Capitalized
    lowerProjectName?: string;

    // Package information
    packageName?: string;
    basePackage?: string;

    // Aggregate information
    aggregateName?: string;
    capitalizedAggregate?: string;
    lowerAggregate?: string;

    // Entity information
    entityName?: string;
    rootEntityName?: string;
    capitalizedEntity?: string;
    lowerEntity?: string;

    // Architecture and features
    architecture?: string;
    features?: string[];
    transactionModel?: string;

    // Generation metadata
    imports?: string[];
    dependencies?: any[];
    methods?: any[];
    fields?: any[];
    properties?: any[];

    // Custom data
    customData?: Record<string, any>;

    // Validation
    requiredFields?: string[];

    // Template-specific data
    [key: string]: any;
}

/**
 * Options for context building
 */
export interface ContextBuildingOptions {
    projectName: string;
    architecture?: string;
    features?: string[];
    outputPath?: string;
    packageSubPath?: string;
    includeImports?: boolean;
    includeDependencies?: boolean;
    customValidation?: (context: TemplateContext) => string[];
}

/**
 * Fluent template context builder with comprehensive functionality
 */
export class TemplateContextBuilder {
    private context: TemplateContext = {};
    private validationRules: Array<(context: TemplateContext) => string[]> = [];
    private packageBuilder: PackageNameBuilder;

    constructor() {
        this.packageBuilder = PackageBuilderFactory.createStandard();
    }

    /**
     * Start building a new context
     */
    static create(): TemplateContextBuilder {
        return new TemplateContextBuilder();
    }

    /**
     * Project-related methods
     */
    withProject(projectName: string): this {
        this.context.projectName = projectName;
        this.context.ProjectName = this.capitalize(projectName);
        this.context.lowerProjectName = projectName.toLowerCase();
        this.context.basePackage = getGlobalConfig().getBasePackage();
        return this;
    }

    withArchitecture(architecture: string): this {
        this.context.architecture = architecture;
        return this;
    }

    withFeatures(features: string[]): this {
        this.context.features = [...features];
        return this;
    }

    withTransactionModel(model: string = 'SAGAS'): this {
        this.context.transactionModel = model;
        return this;
    }

    /**
     * Aggregate-related methods
     */
    withAggregate(aggregate: Aggregate): this {
        const aggregateName = aggregate.name;
        this.context.aggregateName = aggregateName;
        this.context.capitalizedAggregate = this.capitalize(aggregateName);
        this.context.lowerAggregate = aggregateName.toLowerCase();

        // Add aggregate-specific data
        this.context.customData = this.context.customData || {};
        this.context.customData.aggregate = aggregate;

        return this;
    }

    withEntity(entity: Entity): this {
        const entityName = entity.name;
        this.context.entityName = entityName;
        this.context.capitalizedEntity = this.capitalize(entityName);
        this.context.lowerEntity = entityName.toLowerCase();

        // Add entity-specific data
        this.context.customData = this.context.customData || {};
        this.context.customData.entity = entity;

        return this;
    }

    withRootEntity(aggregate: Aggregate): this {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            ErrorHandler.handle(
                new Error(`No root entity found in aggregate ${aggregate.name}`),
                ErrorUtils.aggregateContext('find root entity', aggregate.name, 'context-builder'),
                ErrorSeverity.FATAL
            );
            return this; // Never reached due to FATAL
        }

        this.context.rootEntityName = rootEntity.name;
        return this.withEntity(rootEntity);
    }

    /**
     * Package-related methods
     */
    withPackage(packageName: string): this {
        this.context.packageName = packageName;
        return this;
    }

    withMicroservicePackage(projectName: string, aggregate: string, component: string): this {
        const packageName = this.packageBuilder.buildMicroservicePackage(projectName, aggregate, component);
        return this.withPackage(packageName);
    }

    withCoordinationPackage(projectName: string, component: string): this {
        const packageName = this.packageBuilder.buildCoordinationPackage(projectName, component);
        return this.withPackage(packageName);
    }

    withSagaPackage(projectName: string, component: string): this {
        const packageName = this.packageBuilder.buildSagaPackage(projectName, component);
        return this.withPackage(packageName);
    }

    withSharedPackage(projectName: string, component: string): this {
        const packageName = this.packageBuilder.buildSharedPackage(projectName, component);
        return this.withPackage(packageName);
    }

    /**
     * Data collection methods
     */
    withImports(imports: string[]): this {
        this.context.imports = [...imports];
        return this;
    }

    withDependencies(dependencies: any[]): this {
        this.context.dependencies = [...dependencies];
        return this;
    }

    withMethods(methods: any[]): this {
        this.context.methods = [...methods];
        return this;
    }

    withFields(fields: any[]): this {
        this.context.fields = [...fields];
        return this;
    }

    withProperties(entity: Entity): this {
        if (!entity.properties) {
            this.context.properties = [];
            return this;
        }

        this.context.properties = entity.properties.map((property: any) => {
            const propertyName = property.name;
            const propertyType = UnifiedTypeResolver.resolve(property.type);
            const capitalizedName = this.capitalize(propertyName);

            return {
                name: propertyName,
                type: propertyType,
                capitalizedName,
                getter: `get${capitalizedName}`,
                setter: `set${capitalizedName}`,
                isId: propertyName.toLowerCase() === 'id',
                isCollection: UnifiedTypeResolver.isCollectionType(property.type),
                isEntity: UnifiedTypeResolver.isEntityType(property.type),
                isPrimitive: UnifiedTypeResolver.isPrimitiveType(propertyType),
                originalProperty: property
            };
        });

        return this;
    }

    /**
     * Custom data methods
     */
    withCustomData(key: string, value: any): this {
        if (!this.context.customData) {
            this.context.customData = {};
        }
        this.context.customData[key] = value;
        return this;
    }

    withCustomDataObject(data: Record<string, any>): this {
        if (!this.context.customData) {
            this.context.customData = {};
        }
        Object.assign(this.context.customData, data);
        return this;
    }

    /**
     * Validation methods
     */
    requireFields(...fields: string[]): this {
        if (!this.context.requiredFields) {
            this.context.requiredFields = [];
        }
        this.context.requiredFields.push(...fields);
        return this;
    }

    addValidation(validator: (context: TemplateContext) => string[]): this {
        this.validationRules.push(validator);
        return this;
    }

    /**
     * Specialized context builders for common patterns
     */
    forMicroservice(projectName: string, aggregate: Aggregate, component: string): this {
        return this
            .withProject(projectName)
            .withAggregate(aggregate)
            .withRootEntity(aggregate)
            .withMicroservicePackage(projectName, aggregate.name, component)
            .withProperties(this.context.customData?.entity || aggregate.entities.find((e: any) => e.isRoot)!);
    }

    forCoordination(projectName: string, aggregate: Aggregate, component: string): this {
        return this
            .withProject(projectName)
            .withAggregate(aggregate)
            .withRootEntity(aggregate)
            .withCoordinationPackage(projectName, component);
    }

    forSaga(projectName: string, aggregate: Aggregate, component: string): this {
        return this
            .withProject(projectName)
            .withAggregate(aggregate)
            .withRootEntity(aggregate)
            .withSagaPackage(projectName, component);
    }

    forWebApi(projectName: string, aggregate: Aggregate): this {
        return this
            .withProject(projectName)
            .withAggregate(aggregate)
            .withRootEntity(aggregate)
            .withCoordinationPackage(projectName, 'webapi');
    }

    forValidation(projectName: string, aggregate: Aggregate, validationType: string): this {
        return this
            .withProject(projectName)
            .withAggregate(aggregate)
            .withRootEntity(aggregate)
            .withCoordinationPackage(projectName, `validation.${validationType}`);
    }

    /**
     * Build and validate the context
     */
    build(): TemplateContext {
        const context = { ...this.context };

        // Run validation
        const errors = this.validate(context);
        if (errors.length > 0) {
            ErrorHandler.handle(
                new Error(`Context validation failed: ${errors.join(', ')}`),
                ErrorUtils.aggregateContext(
                    'validate template context',
                    context.aggregateName || 'unknown',
                    'context-builder',
                    { errors, context: Object.keys(context) }
                ),
                ErrorSeverity.FATAL
            );
        }

        return context;
    }

    /**
     * Build without validation (for partial contexts)
     */
    buildPartial(): TemplateContext {
        return { ...this.context };
    }

    /**
     * Reset the builder for reuse
     */
    reset(): this {
        this.context = {};
        this.validationRules = [];
        return this;
    }

    /**
     * Clone the current builder
     */
    clone(): TemplateContextBuilder {
        const clone = new TemplateContextBuilder();
        clone.context = { ...this.context };
        clone.validationRules = [...this.validationRules];
        return clone;
    }

    /**
     * Private helper methods
     */
    private validate(context: TemplateContext): string[] {
        const errors: string[] = [];

        // Check required fields
        if (context.requiredFields) {
            for (const field of context.requiredFields) {
                if (!context[field]) {
                    errors.push(`Missing required field: ${field}`);
                }
            }
        }

        // Run custom validation rules
        for (const validator of this.validationRules) {
            const validationErrors = validator(context);
            errors.push(...validationErrors);
        }

        return errors;
    }

    private capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
}

/**
 * Factory for creating context builders with common configurations
 */
export class ContextBuilderFactory {
    /**
     * Create a context builder for entity generation
     */
    static forEntity(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'aggregate')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    /**
     * Create a context builder for service generation
     */
    static forService(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'service')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    /**
     * Create a context builder for repository generation
     */
    static forRepository(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'repository')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    /**
     * Create a context builder for controller generation
     */
    static forController(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forWebApi(projectName, aggregate)
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    /**
     * Create a context builder for event generation
     */
    static forEvent(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'events')
            .requireFields('projectName', 'aggregateName', 'packageName');
    }

    /**
     * Create a context builder for saga generation
     */
    static forSaga(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forSaga(projectName, aggregate, 'aggregates')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    /**
     * Create a context builder for validation generation
     */
    static forValidation(projectName: string, aggregate: Aggregate, validationType: string): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forValidation(projectName, aggregate, validationType)
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    /**
     * Create a context builder for configuration generation
     */
    static forConfiguration(projectName: string, architecture: string = 'default'): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .withProject(projectName)
            .withArchitecture(architecture)
            .requireFields('projectName', 'architecture');
    }
}

/**
 * Utility functions for common context operations
 */
export class ContextUtils {
    /**
     * Merge multiple contexts
     */
    static merge(...contexts: TemplateContext[]): TemplateContext {
        const merged: TemplateContext = {};

        for (const context of contexts) {
            Object.assign(merged, context);

            // Merge arrays
            if (context.imports && merged.imports) {
                merged.imports = [...new Set([...merged.imports, ...context.imports])];
            }
            if (context.dependencies && merged.dependencies) {
                merged.dependencies = [...merged.dependencies, ...context.dependencies];
            }
            if (context.features && merged.features) {
                merged.features = [...new Set([...merged.features, ...context.features])];
            }

            // Merge custom data
            if (context.customData && merged.customData) {
                merged.customData = { ...merged.customData, ...context.customData };
            }
        }

        return merged;
    }

    /**
     * Extract common naming patterns from context
     */
    static extractNaming(context: TemplateContext): Record<string, string> {
        return {
            projectName: context.projectName || '',
            ProjectName: context.ProjectName || '',
            lowerProjectName: context.lowerProjectName || '',
            aggregateName: context.aggregateName || '',
            capitalizedAggregate: context.capitalizedAggregate || '',
            lowerAggregate: context.lowerAggregate || '',
            entityName: context.entityName || '',
            capitalizedEntity: context.capitalizedEntity || '',
            lowerEntity: context.lowerEntity || '',
            rootEntityName: context.rootEntityName || ''
        };
    }

    /**
     * Validate context has required fields for specific generator type
     */
    static validateForGenerator(context: TemplateContext, generatorType: string): string[] {
        const errors: string[] = [];

        const requirements: Record<string, string[]> = {
            'entity': ['projectName', 'aggregateName', 'packageName', 'rootEntityName'],
            'service': ['projectName', 'aggregateName', 'packageName', 'rootEntityName'],
            'repository': ['projectName', 'aggregateName', 'packageName', 'rootEntityName'],
            'controller': ['projectName', 'aggregateName', 'packageName', 'rootEntityName'],
            'event': ['projectName', 'aggregateName', 'packageName'],
            'saga': ['projectName', 'aggregateName', 'packageName', 'rootEntityName'],
            'validation': ['projectName', 'aggregateName', 'packageName', 'rootEntityName'],
            'configuration': ['projectName', 'architecture']
        };

        const required = requirements[generatorType] || [];
        for (const field of required) {
            if (!context[field]) {
                errors.push(`Missing required field for ${generatorType}: ${field}`);
            }
        }

        return errors;
    }
}
