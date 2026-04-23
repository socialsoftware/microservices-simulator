


import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "./unified-type-resolver.js";
import { PackageNameBuilder, PackageBuilderFactory } from "../../utils/package-name-builder.js";
import { getGlobalConfig } from "./config.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../utils/error-handler.js";
import { StringUtils } from '../../utils/string-utils.js';



export interface TemplateContext {
    
    projectName?: string;
    ProjectName?: string; 
    lowerProjectName?: string;

    
    packageName?: string;
    basePackage?: string;

    
    aggregateName?: string;
    capitalizedAggregate?: string;
    lowerAggregate?: string;

    
    entityName?: string;
    rootEntityName?: string;
    capitalizedEntity?: string;
    lowerEntity?: string;

    
    architecture?: string;
    features?: string[];
    transactionModel?: string;

    
    imports?: string[];
    dependencies?: any[];
    methods?: any[];
    fields?: any[];
    properties?: any[];

    
    customData?: Record<string, any>;

    
    requiredFields?: string[];

    
    [key: string]: any;
}



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



export class TemplateContextBuilder {
    private context: TemplateContext = {};
    private validationRules: Array<(context: TemplateContext) => string[]> = [];
    private packageBuilder: PackageNameBuilder;

    constructor() {
        this.packageBuilder = PackageBuilderFactory.createStandard();
    }

    

    static create(): TemplateContextBuilder {
        return new TemplateContextBuilder();
    }

    

    withProject(projectName: string): this {
        this.context.projectName = projectName;
        this.context.ProjectName = StringUtils.capitalize(projectName);
        this.context.lowerProjectName = projectName.toLowerCase();
        this.context.basePackage = getGlobalConfig().getBasePackage();
        return this;
    }

    withArchitecture(architecture: string): this {
        this.context.architecture = architecture;
        return this;
    }

    withFeatures(features: string[]): this {
        
        
        return this;
    }

    withTransactionModel(model: string = 'SAGAS'): this {
        this.context.transactionModel = model;
        return this;
    }

    

    withAggregate(aggregate: Aggregate): this {
        const aggregateName = aggregate.name;
        this.context.aggregateName = aggregateName;
        this.context.capitalizedAggregate = StringUtils.capitalize(aggregateName);
        this.context.lowerAggregate = aggregateName.toLowerCase();

        
        this.context.customData = this.context.customData || {};
        this.context.customData.aggregate = aggregate;

        return this;
    }

    withEntity(entity: Entity): this {
        const entityName = entity.name;
        this.context.entityName = entityName;
        this.context.capitalizedEntity = StringUtils.capitalize(entityName);
        this.context.lowerEntity = entityName.toLowerCase();

        
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
            return this; 
        }

        this.context.rootEntityName = rootEntity.name;
        return this.withEntity(rootEntity);
    }

    

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
            const capitalizedName = StringUtils.capitalize(propertyName);

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

    

    build(): TemplateContext {
        const context = { ...this.context };

        
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

    

    buildPartial(): TemplateContext {
        return { ...this.context };
    }

    

    reset(): this {
        this.context = {};
        this.validationRules = [];
        return this;
    }

    

    clone(): TemplateContextBuilder {
        const clone = new TemplateContextBuilder();
        clone.context = { ...this.context };
        clone.validationRules = [...this.validationRules];
        return clone;
    }

    

    private validate(context: TemplateContext): string[] {
        const errors: string[] = [];

        
        if (context.requiredFields) {
            for (const field of context.requiredFields) {
                if (!context[field]) {
                    errors.push(`Missing required field: ${field}`);
                }
            }
        }

        
        for (const validator of this.validationRules) {
            const validationErrors = validator(context);
            errors.push(...validationErrors);
        }

        return errors;
    }
}



export class ContextBuilderFactory {
    

    static forEntity(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'aggregate')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    

    static forService(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'service')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    

    static forRepository(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'repository')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    

    static forController(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forWebApi(projectName, aggregate)
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    

    static forEvent(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forMicroservice(projectName, aggregate, 'events')
            .requireFields('projectName', 'aggregateName', 'packageName');
    }

    

    static forSaga(projectName: string, aggregate: Aggregate): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forSaga(projectName, aggregate, 'aggregates')
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    

    static forValidation(projectName: string, aggregate: Aggregate, validationType: string): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .forValidation(projectName, aggregate, validationType)
            .requireFields('projectName', 'aggregateName', 'packageName', 'rootEntityName');
    }

    

    static forConfiguration(projectName: string, architecture: string = 'default'): TemplateContextBuilder {
        return TemplateContextBuilder.create()
            .withProject(projectName)
            .withArchitecture(architecture)
            .requireFields('projectName', 'architecture');
    }
}



export class ContextUtils {
    

    static merge(...contexts: TemplateContext[]): TemplateContext {
        const merged: TemplateContext = {};

        for (const context of contexts) {
            Object.assign(merged, context);

            
            if (context.imports && merged.imports) {
                merged.imports = [...new Set([...merged.imports, ...context.imports])];
            }
            if (context.dependencies && merged.dependencies) {
                merged.dependencies = [...merged.dependencies, ...context.dependencies];
            }
            
            
            
            

            
            if (context.customData && merged.customData) {
                merged.customData = { ...merged.customData, ...context.customData };
            }
        }

        return merged;
    }

    

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
