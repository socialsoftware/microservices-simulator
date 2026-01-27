import { Aggregate, Entity } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { EntityRegistry } from '../common/utils/entity-registry.js';
import { OrchestrationBase } from '../common/orchestration-base.js';
import { FunctionalitiesCrudGenerator } from './functionalities-crud-generator.js';
import { FunctionalitiesImportsBuilder } from './functionalities-imports-builder.js';
import { FunctionalitiesMethodGenerator } from './functionalities-method-generator.js';
import { TypeResolver } from '../common/resolvers/type-resolver.js';

/**
 * Main orchestrator for functionalities class generation.
 * Delegates to specialized generators for different concerns.
 */
export class FunctionalitiesGenerator extends OrchestrationBase {
    private crudGenerator = new FunctionalitiesCrudGenerator();
    private importsBuilder = new FunctionalitiesImportsBuilder();
    private methodGenerator = new FunctionalitiesMethodGenerator();

    // Expose crudGenerator methods for use in buildBusinessMethods
    getCrudGenerator() {
        return this.crudGenerator;
    }

    /**
     * Generate a functionalities class for an aggregate
     */
    async generate(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions, allAggregates?: Aggregate[]): Promise<string> {
        const entityRegistry = allAggregates ?
            EntityRegistry.buildFromAggregates(allAggregates) :
            EntityRegistry.buildFromAggregates([aggregate]);

        const context = this.buildContext(aggregate, rootEntity, options, entityRegistry);
        const template = this.loadTemplate('coordination/functionalities.hbs');
        return this.renderTemplate(template, context);
    }

    /**
     * Build template context for rendering
     */
    private buildContext(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions, entityRegistry: EntityRegistry): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const projectName = options.projectName.toLowerCase();
        const ProjectName = this.capitalize(options.projectName);

        const consistencyModels = options.consistencyModels || ['sagas'];
        const allAggregates = entityRegistry.getAllAggregates();

        const dependencies = this.buildDependencies(aggregate, options, rootEntity, allAggregates);
        const businessMethods = this.buildBusinessMethods(aggregate, rootEntity, capitalizedAggregate, entityRegistry, consistencyModels, allAggregates);
        const checkInputMethod = this.buildCheckInputMethod(aggregate, rootEntity, capitalizedAggregate, lowerAggregate, options.projectName);
        const imports = this.importsBuilder.buildImports(aggregate, rootEntity, options, dependencies, entityRegistry, businessMethods);

        const basePackage = this.getBasePackage();
        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${basePackage}.${projectName}.coordination.functionalities`,
            basePackage,
            annotations: this.getFrameworkAnnotations(),
            transactionModel: this.getTransactionModel(),
            imports,
            dependencies,
            businessMethods,
            checkInputMethod,
            projectName,
            ProjectName,
            consistencyModels,
            hasSagas: options.architecture === 'causal-saga' || options.features?.includes('sagas'),
            hasExternalDtos: options.architecture === 'default'
        };
    }

    /**
     * Build dependencies for the functionalities class
     */
    private buildDependencies(aggregate: Aggregate, options: CoordinationGenerationOptions, rootEntity: Entity, allAggregates?: Aggregate[]): any[] {
        const dependencies: any[] = [];
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();

        dependencies.push({
            name: `${lowerAggregate}Service`,
            type: `${this.capitalize(aggregateName)}Service`,
            required: true
        });

        const needsUserService = this.checkUserServiceUsage(aggregate);
        if (needsUserService && lowerAggregate !== 'user') {
            dependencies.push({
                name: 'userService',
                type: 'UserService',
                required: true
            });
        }

        // Note: With the new approach, cross-aggregate DTOs are passed directly in the CreateRequestDto,
        // so we no longer need to inject cross-aggregate services for create operations.
        // The saga will receive the full DTOs and create projection entities directly from them.

        dependencies.push({
            name: 'sagaUnitOfWorkService',
            type: 'SagaUnitOfWorkService',
            required: true
        });

        return dependencies;
    }

    /**
     * Build all business methods for the functionalities class
     */
    private buildBusinessMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, entityRegistry: EntityRegistry, consistencyModels: string[], allAggregates?: Aggregate[]): any[] {
        const methods: any[] = [];
        const addedMethods = new Set<string>();
        const lowerAggregate = aggregateName.toLowerCase();

        // 1. Add CRUD methods if generateCrud is enabled
        if (aggregate.generateCrud) {
            const crudMethods = this.crudGenerator.generateCrudMethods(aggregateName, lowerAggregate, rootEntity, aggregate, allAggregates);
            crudMethods.forEach(method => {
                const methodSignature = `${method.name}_${method.parameters.map((p: any) => p.type).join('_')}`;
                if (!addedMethods.has(methodSignature)) {
                    methods.push(method);
                    addedMethods.add(methodSignature);
                }
            });

            // Add cross-aggregate service dependencies for create method
            // Note: We need to call findEntityRelationships and getRelatedDtoType which are private
            // So we'll handle this in buildDependencies instead
        }

        // 2. Add methods from endpoint definitions
        if (aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints) {
            aggregate.webApiEndpoints.endpoints.forEach((endpoint: any) => {
                const params = this.methodGenerator.extractEndpointParameters(endpoint.parameters);
                const returnType = this.methodGenerator.extractReturnType(endpoint.returnType, entityRegistry);
                const methodSignature = `${endpoint.methodName}_${params.map((p: any) => p.type).join('_')}`;
                if (!addedMethods.has(methodSignature)) {
                    methods.push({
                        name: endpoint.methodName,
                        returnType: returnType,
                        parameters: params,
                        body: this.methodGenerator.generateWebApiMethodBody(endpoint, returnType, aggregateName, consistencyModels),
                        throwsException: endpoint.throwsException === 'true'
                    });
                    addedMethods.add(methodSignature);
                }
            });
        }

        // 3. Add methods from functionality definitions
        const functionalities = (aggregate as any).functionalities;
        if (functionalities && functionalities.functionalityMethods) {
            functionalities.functionalityMethods.forEach((func: any) => {
                const params = this.methodGenerator.extractFunctionalityParameters(func.parameters);
                const returnType = this.methodGenerator.extractReturnType(func.returnType, entityRegistry);
                const methodSignature = `${func.name}_${params.map((p: any) => p.type).join('_')}`;
                if (!addedMethods.has(methodSignature)) {
                    methods.push({
                        name: func.name,
                        returnType,
                        parameters: params,
                        body: this.methodGenerator.generateFunctionalityMethodBody(func, returnType, aggregateName),
                        throwsException: false
                    });
                    addedMethods.add(methodSignature);
                }
            });
        }

        return methods;
    }

    /**
     * Check if UserService is needed
     */
    private checkUserServiceUsage(aggregate: Aggregate): boolean {
        if (aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints) {
            return aggregate.webApiEndpoints.endpoints.some((endpoint: any) =>
                endpoint.parameters && endpoint.parameters.some((param: any) =>
                    param.name && param.name.toLowerCase().includes('user')
                )
            );
        }
        return false;
    }

    /**
     * Build checkInput methods for validating DTOs
     * Generates two overloads: one for ExecutionDto (update) and one for CreateRequestDto (create)
     */
    private buildCheckInputMethod(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, lowerAggregate: string, projectName: string): string | null {
        // Check if CRUD is enabled (which means create/update operations will call checkInput)
        const hasCrud = aggregate.generateCrud;
        if (!hasCrud) {
            return null;
        }

        const dtoType = `${aggregateName}Dto`;
        const dtoParamName = `${lowerAggregate}Dto`;
        const createRequestDtoType = `Create${aggregateName}RequestDto`;
        const ProjectName = this.capitalize(projectName);

        if (!rootEntity || !rootEntity.properties) {
            // Generate empty methods if CRUD is enabled but no properties
            return `private void checkInput(${dtoType} ${dtoParamName}) {
}

    private void checkInput(${createRequestDtoType} createRequest) {
}`;
        }

        const validationChecks: string[] = [];
        const createValidationChecks: string[] = [];

        // Find required String fields
        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isString = javaType === 'String';
            const isCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');
            const isEntity = TypeResolver.isEntityType(javaType);
            const isEnum = this.isEnumType(prop.type);

            // Check if it's a required String field (not nullable, not optional, not a collection, not an entity, not an enum)
            if (isString && !isCollection && !isEntity && !isEnum) {
                const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                const exceptionConstant = `${aggregateName.toUpperCase()}_MISSING_${prop.name.toUpperCase()}`;
                
                // For ExecutionDto (update operations)
                validationChecks.push(`        if (${dtoParamName}.get${capitalizedName}() == null) {
            throw new ${ProjectName}Exception(${exceptionConstant});
        }`);
                
                // For CreateRequestDto (create operations)
                createValidationChecks.push(`        if (createRequest.get${capitalizedName}() == null) {
            throw new ${ProjectName}Exception(${exceptionConstant});
        }`);
            }
        }

        // Generate both methods
        let dtoMethod: string;
        if (validationChecks.length === 0) {
            dtoMethod = `private void checkInput(${dtoType} ${dtoParamName}) {
}`;
        } else {
            dtoMethod = `private void checkInput(${dtoType} ${dtoParamName}) {
${validationChecks.join('\n')}
}`;
        }

        let createMethod: string;
        if (createValidationChecks.length === 0) {
            createMethod = `private void checkInput(${createRequestDtoType} createRequest) {
}`;
        } else {
            createMethod = `private void checkInput(${createRequestDtoType} createRequest) {
${createValidationChecks.join('\n')}
}`;
        }

        return `${dtoMethod}

    ${createMethod}`;
    }

    /**
     * Check if a type is an enum
     */
    private isEnumType(type: any): boolean {
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
                return true;
            }
            const ref = type.type.ref;
            if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                return true;
            }
        }
        return false;
    }
}
