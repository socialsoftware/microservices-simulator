import { AggregateExt, EntityExt } from '../../../types/ast-extensions.js';
import { CoordinationGenerationOptions } from '../../microservices/types.js';
import { EntityRegistry } from '../../common/utils/entity-registry.js';
import { GeneratorCapabilities, GeneratorCapabilitiesFactory } from '../../common/generator-capabilities.js';
import { FunctionalitiesCrudGenerator } from './functionalities-crud-generator.js';
import { FunctionalitiesCollectionGenerator } from './functionalities-collection-generator.js';
import { FunctionalitiesImportsBuilder } from './functionalities-imports-builder.js';
import { FunctionalitiesMethodGenerator } from './functionalities-method-generator.js';
import { UnifiedTypeResolver as TypeResolver } from '../../common/unified-type-resolver.js';
import { StringUtils } from '../../../utils/string-utils.js';

/**
 * Main orchestrator for functionalities class generation.
 * Delegates to specialized generators for different concerns.
 */
export class FunctionalitiesGenerator {
    private capabilities: GeneratorCapabilities;
    private crudGenerator = new FunctionalitiesCrudGenerator();
    private collectionGenerator = new FunctionalitiesCollectionGenerator();
    private importsBuilder = new FunctionalitiesImportsBuilder();
    private methodGenerator = new FunctionalitiesMethodGenerator();

    constructor(capabilities?: GeneratorCapabilities) {
        this.capabilities = capabilities || GeneratorCapabilitiesFactory.createWebApiCapabilities();
    }


    private getBasePackage(): string {
        return this.capabilities.packageBuilder.buildCustomPackage('').split('.').slice(0, -1).join('.');
    }

    private getFrameworkAnnotations(): any {
        return {
            service: '@Service',
            repository: '@Repository',
            component: '@Component',
            transactional: '@Transactional',
            autowired: '@Autowired',
            inject: '@Inject',
            controller: '@Controller',
            restController: '@RestController'
        };
    }

    private getTransactionModel(): string {
        return 'SAGAS';
    }

    private loadTemplate(templatePath: string): string {
        // Templates are loaded and rendered by capabilities.templateRenderer
        return templatePath;
    }

    private renderTemplate(templatePath: string, context: any): string {
        return this.capabilities.templateRenderer.render(templatePath, context);
    }

    // Expose crudGenerator methods for use in buildBusinessMethods
    getCrudGenerator() {
        return this.crudGenerator;
    }

    /**
     * Generate a functionalities class for an aggregate
     */
    async generate(aggregate: AggregateExt, rootEntity: EntityExt, options: CoordinationGenerationOptions, allAggregates?: AggregateExt[]): Promise<string> {
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
    private buildContext(aggregate: AggregateExt, rootEntity: EntityExt, options: CoordinationGenerationOptions, entityRegistry: EntityRegistry): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const projectName = options.projectName.toLowerCase();
        const ProjectName = StringUtils.capitalize(options.projectName);

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
            hasSagas: options.architecture === 'causal-saga',
            hasExternalDtos: options.architecture === 'default'
        };
    }

    /**
     * Build dependencies for the functionalities class
     */
    private buildDependencies(aggregate: AggregateExt, options: CoordinationGenerationOptions, rootEntity: EntityExt, allAggregates?: AggregateExt[]): any[] {
        const dependencies: any[] = [];
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();

        dependencies.push({
            name: `${lowerAggregate}Service`,
            type: `${StringUtils.capitalize(aggregateName)}Service`,
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
    private buildBusinessMethods(aggregate: AggregateExt, rootEntity: EntityExt, aggregateName: string, entityRegistry: EntityRegistry, consistencyModels: string[], allAggregates?: AggregateExt[]): any[] {
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

            // 1.5. Add collection methods if generateCrud is enabled
            const collectionMethods = this.collectionGenerator.generateCollectionMethods(aggregateName, lowerAggregate, rootEntity, aggregate);
            collectionMethods.forEach(method => {
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
    private checkUserServiceUsage(aggregate: AggregateExt): boolean {
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
    private buildCheckInputMethod(aggregate: AggregateExt, rootEntity: EntityExt, aggregateName: string, lowerAggregate: string, projectName: string): string | null {
        // Check if CRUD is enabled (which means create/update operations will call checkInput)
        const hasCrud = aggregate.generateCrud;
        if (!hasCrud) {
            return null;
        }

        const dtoType = `${aggregateName}Dto`;
        const dtoParamName = `${lowerAggregate}Dto`;
        const createRequestDtoType = `Create${aggregateName}RequestDto`;
        const ProjectName = StringUtils.capitalize(projectName);

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
