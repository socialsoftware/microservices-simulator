import { Aggregate, Entity } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { EntityRegistry } from '../common/utils/entity-registry.js';
import { OrchestrationBase } from '../common/orchestration-base.js';
import { FunctionalitiesCrudGenerator } from './functionalities-crud-generator.js';
import { FunctionalitiesImportsBuilder } from './functionalities-imports-builder.js';
import { FunctionalitiesMethodGenerator } from './functionalities-method-generator.js';

/**
 * Main orchestrator for functionalities class generation.
 * Delegates to specialized generators for different concerns.
 */
export class FunctionalitiesGenerator extends OrchestrationBase {
    private crudGenerator = new FunctionalitiesCrudGenerator();
    private importsBuilder = new FunctionalitiesImportsBuilder();
    private methodGenerator = new FunctionalitiesMethodGenerator();

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

        const dependencies = this.buildDependencies(aggregate, options);
        const businessMethods = this.buildBusinessMethods(aggregate, rootEntity, capitalizedAggregate, entityRegistry, consistencyModels);
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
    private buildDependencies(aggregate: Aggregate, options: CoordinationGenerationOptions): any[] {
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
    private buildBusinessMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, entityRegistry: EntityRegistry, consistencyModels: string[]): any[] {
        const methods: any[] = [];
        const addedMethods = new Set<string>();
        const lowerAggregate = aggregateName.toLowerCase();

        // 1. Add CRUD methods if generateCrud is enabled
        if ((aggregate.webApiEndpoints as any)?.generateCrud) {
            const crudMethods = this.crudGenerator.generateCrudMethods(aggregateName, lowerAggregate, rootEntity);
            crudMethods.forEach(method => {
                const methodSignature = `${method.name}_${method.parameters.map((p: any) => p.type).join('_')}`;
                if (!addedMethods.has(methodSignature)) {
                    methods.push(method);
                    addedMethods.add(methodSignature);
                }
            });
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
}
