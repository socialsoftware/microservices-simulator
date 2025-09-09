import { Entity, Aggregate } from "../../../language/generated/ast.js";
import { TypeResolver } from "./type-resolver.js";
import { SimpleTemplateEngine } from "../template-engine/simple-engine.js";
import { Storage } from "../template-engine/storage.js";
import { Utils } from "../../utils/generator-utils.js";

export interface BaseGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export abstract class BaseGenerator {
    protected typeResolver: TypeResolver;
    protected templateEngine: SimpleTemplateEngine;
    protected storage: Storage;

    constructor() {
        this.typeResolver = new TypeResolver();
        this.templateEngine = new SimpleTemplateEngine();
        this.storage = new Storage({
            templatesPath: './templates',
            cacheEnabled: true,
            autoReload: false,
            maxCacheSize: 100
        });
    }

    protected buildEntityContext(entity: Entity, options: BaseGenerationOptions): any {
        const fields = entity.properties.map(property => {
            const javaType = TypeResolver.resolveJavaType(property.type);
            return {
                name: property.name,
                type: javaType,
                isPrivate: true,
                isProtected: false,
                capitalizedName: Utils.capitalize(property.name)
            };
        });

        const constructorParams = entity.properties.map(property => {
            const javaType = TypeResolver.resolveJavaType(property.type);
            return {
                type: javaType,
                name: property.name
            };
        });

        const constructorAssignments = entity.properties.map(property => {
            const javaType = TypeResolver.resolveJavaType(property.type);
            if (javaType === 'LocalDateTime') {
                return `set${Utils.capitalize(property.name)}(DateHandler.toLocalDateTime(${property.name}));`;
            } else {
                return `set${Utils.capitalize(property.name)}(${property.name});`;
            }
        });

        return {
            entityName: entity.name,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            tableName: entity.name.toLowerCase(),
            fields,
            constructorParams,
            constructorAssignments,
            isRoot: entity.isRoot
        };
    }

    protected buildAggregateContext(aggregate: Aggregate, options: BaseGenerationOptions): any {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        return {
            aggregateName: aggregate.name,
            rootEntity: rootEntity,
            entities: aggregate.entities,
            projectName: options.projectName,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            ...this.buildEntityContext(rootEntity, options)
        };
    }

    protected buildDtoContext(entity: Entity, options: BaseGenerationOptions): any {
        const fields = entity.properties.map(property => {
            const javaType = TypeResolver.resolveJavaType(property.type);
            return {
                name: property.name,
                type: javaType,
                capitalizedName: Utils.capitalize(property.name)
            };
        });

        const constructorParams = entity.properties.map(property => {
            const javaType = TypeResolver.resolveJavaType(property.type);
            return {
                type: javaType,
                name: property.name
            };
        });

        const constructorAssignments = entity.properties.map(property => {
            return `this.${property.name} = ${property.name};`;
        });

        return {
            entityName: entity.name,
            dtoName: `${entity.name}Dto`,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            fields,
            constructorParams,
            constructorAssignments
        };
    }

    protected buildServiceContext(aggregate: Aggregate, options: BaseGenerationOptions): any {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const methods = this.buildServiceMethods(rootEntity, aggregate.name);

        return {
            serviceName: `${aggregate.name}Service`,
            repositoryName: `${aggregate.name}Repository`,
            repositoryVariable: `${aggregate.name.toLowerCase()}Repository`,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            methods
        };
    }

    protected buildRepositoryContext(aggregate: Aggregate, options: BaseGenerationOptions): any {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const queries = this.buildRepositoryQueries(rootEntity, aggregate.name);

        return {
            repositoryName: `${aggregate.name}Repository`,
            entityName: rootEntity.name,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            queries
        };
    }

    protected buildFactoryContext(aggregate: Aggregate, options: BaseGenerationOptions): any {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const constructorParams = rootEntity.properties.map(property => {
            const javaType = TypeResolver.resolveJavaType(property.type);
            return {
                type: javaType,
                name: property.name
            };
        });

        return {
            factoryName: `${aggregate.name}Factory`,
            entityName: rootEntity.name,
            dtoName: `${rootEntity.name}Dto`,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            constructorParams
        };
    }

    protected buildValidationContext(aggregate: Aggregate, options: BaseGenerationOptions): any {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const invariants = this.buildInvariants(rootEntity);
        const businessRules = this.buildBusinessRules(aggregate);

        return {
            validationName: `${aggregate.name}Validation`,
            entityName: rootEntity.name,
            entityVariable: rootEntity.name.toLowerCase(),
            projectName: options.projectName,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            invariants,
            businessRules
        };
    }

    protected buildWebApiContext(aggregate: Aggregate, options: BaseGenerationOptions): any {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const endpoints = this.buildWebApiEndpoints(rootEntity, aggregate.name);

        return {
            controllerName: `${aggregate.name}Controller`,
            serviceName: `${aggregate.name}Service`,
            serviceVariable: `${aggregate.name.toLowerCase()}Service`,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName}`,
            aggregateName: aggregate.name.toLowerCase(),
            endpoints
        };
    }

    protected buildServiceMethods(entity: Entity, aggregateName: string): any[] {
        const methods: any[] = [];

        methods.push({
            methodName: `create${entity.name}`,
            returnType: entity.name,
            parameters: entity.properties.map((prop: any) => ({
                type: TypeResolver.resolveJavaType(prop.type),
                name: prop.name
            }))
        });

        methods.push({
            methodName: `get${entity.name}ById`,
            returnType: entity.name,
            parameters: [{
                type: 'Integer',
                name: 'id'
            }]
        });

        methods.push({
            methodName: `getAll${entity.name}s`,
            returnType: `List<${entity.name}>`,
            parameters: []
        });

        return methods;
    }

    protected buildRepositoryQueries(entity: Entity, aggregateName: string): any[] {
        const queries: any[] = [];

        queries.push({
            methodName: `findBy${Utils.capitalize(entity.properties[0]?.name || 'Id')}`,
            query: `SELECT e FROM ${entity.name} e WHERE e.${entity.properties[0]?.name || 'id'} = :${entity.properties[0]?.name || 'id'}`,
            parameters: [{
                type: TypeResolver.resolveJavaType(entity.properties[0]?.type || 'Integer'),
                name: entity.properties[0]?.name || 'id'
            }]
        });

        return queries;
    }

    protected buildInvariants(entity: Entity): any[] {
        const invariants: any[] = [];

        entity.properties.forEach((property: any) => {
            if (TypeResolver.resolveJavaType(property.type) === 'String') {
                invariants.push({
                    condition: `${property.name} != null && !${property.name}.isEmpty()`,
                    errorCode: `INVALID_${property.name.toUpperCase()}`
                });
            }
        });

        return invariants;
    }

    protected buildBusinessRules(aggregate: Aggregate): any[] {
        const businessRules: any[] = [];

        businessRules.push({
            ruleName: 'ValidateBusinessLogic',
            description: 'Validate business logic for aggregate'
        });

        return businessRules;
    }

    protected buildWebApiEndpoints(entity: Entity, aggregateName: string): any[] {
        const endpoints: any[] = [];

        endpoints.push({
            methodName: `create${entity.name}`,
            httpMethod: 'PostMapping',
            path: '',
            returnType: entity.name,
            parameters: [{
                annotation: '@RequestBody',
                type: `${entity.name}Dto`,
                name: 'dto'
            }],
            serviceCall: `${aggregateName.toLowerCase()}Service.create${entity.name}(dto)`
        });

        endpoints.push({
            methodName: `get${entity.name}ById`,
            httpMethod: 'GetMapping',
            path: '/{id}',
            returnType: entity.name,
            parameters: [{
                annotation: '@PathVariable',
                type: 'Integer',
                name: 'id'
            }],
            serviceCall: `${aggregateName.toLowerCase()}Service.get${entity.name}ById(id)`
        });

        return endpoints;
    }

    protected renderTemplate(template: string, context: any): string {
        try {
            return this.templateEngine.render(template, context);
        } catch (error) {
            console.error(`Error rendering template:`, error);
            throw error;
        }
    }

    protected capitalize(str: string): string {
        return Utils.capitalize(str);
    }

    protected resolveJavaType(fieldType: any): string {
        return TypeResolver.resolveJavaType(fieldType);
    }
}
