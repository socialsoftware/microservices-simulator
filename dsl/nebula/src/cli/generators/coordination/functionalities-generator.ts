import { Aggregate, Entity } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { TypeResolver } from '../common/resolvers/type-resolver.js';
import { EntityRegistry } from '../common/utils/entity-registry.js';
import { OrchestrationBase } from '../common/orchestration-base.js';

export class FunctionalitiesGenerator extends OrchestrationBase {
    async generate(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions, allAggregates?: Aggregate[]): Promise<string> {
        const entityRegistry = allAggregates ?
            EntityRegistry.buildFromAggregates(allAggregates) :
            EntityRegistry.buildFromAggregates([aggregate]);

        const context = this.buildContext(aggregate, rootEntity, options, entityRegistry);
        const template = this.loadTemplate('coordination/functionalities.hbs');
        return this.renderTemplate(template, context);
    }

    private buildContext(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions, entityRegistry: EntityRegistry): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const projectName = options.projectName.toLowerCase();
        const ProjectName = this.capitalize(options.projectName);

        // Get consistency models from configuration, default to sagas only
        const consistencyModels = options.consistencyModels || ['sagas'];

        const dependencies = this.buildDependencies(aggregate, options);
        const businessMethods = this.buildBusinessMethods(aggregate, rootEntity, capitalizedAggregate, entityRegistry, consistencyModels);
        const imports = this.buildImports(aggregate, rootEntity, options, dependencies, entityRegistry, businessMethods);

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

    private buildDependencies(aggregate: Aggregate, options: CoordinationGenerationOptions): any[] {
        const dependencies: any[] = [];
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();

        // Main aggregate service (e.g., AnswerService, UserService)
        dependencies.push({
            name: `${lowerAggregate}Service`,
            type: `${this.capitalize(aggregateName)}Service`,
            required: true
        });

        // Additional UserService only for aggregates that are NOT the User aggregate itself
        const needsUserService = this.checkUserServiceUsage(aggregate);
        if (needsUserService && lowerAggregate !== 'user') {
            dependencies.push({
                name: 'userService',
                type: 'UserService',
                required: true
            });
        }

        // Saga unit of work service
        dependencies.push({
            name: 'sagaUnitOfWorkService',
            type: 'SagaUnitOfWorkService',
            required: true
        });

        // Factory is useful for most aggregates, but we can skip it for the User aggregate where it isn't used
        if (lowerAggregate !== 'user') {
            dependencies.push({
                name: `${lowerAggregate}Factory`,
                type: `${this.capitalize(aggregateName)}Factory`,
                required: true
            });
        }

        return dependencies;
    }

    private buildBusinessMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, entityRegistry: EntityRegistry, consistencyModels: string[]): any[] {
        const methods: any[] = [];
        const addedMethods = new Set<string>();
        const lowerAggregate = aggregateName.toLowerCase();

        if ((aggregate.webApiEndpoints as any)?.autoCrud) {
            const crudMethods = this.generateCrudMethods(aggregateName, lowerAggregate, consistencyModels);
            crudMethods.forEach(method => {
                const methodSignature = `${method.name}_${method.parameters.map((p: any) => p.type).join('_')}`;
                if (!addedMethods.has(methodSignature)) {
                    methods.push(method);
                    addedMethods.add(methodSignature);
                }
            });
        }

        // Add methods from WebAPIEndpoints
        if (aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints) {
            aggregate.webApiEndpoints.endpoints.forEach((endpoint: any) => {
                const params = this.extractEndpointParameters(endpoint.parameters);
                const returnType = this.extractReturnType(endpoint.returnType, entityRegistry);
                const methodSignature = `${endpoint.methodName}_${params.map((p: any) => p.type).join('_')}`;
                if (!addedMethods.has(methodSignature)) {
                    methods.push({
                        name: endpoint.methodName,
                        returnType: returnType,
                        parameters: params,
                        body: this.generateWebApiMethodBody(endpoint, returnType, aggregateName, consistencyModels),
                        throwsException: endpoint.throwsException === 'true'
                    });
                    addedMethods.add(methodSignature);
                }
            });
        }

        const functionalities = (aggregate as any).functionalities;
        if (functionalities && functionalities.functionalityMethods) {
            functionalities.functionalityMethods.forEach((func: any) => {
                const params = this.extractFunctionalityParameters(func.parameters);
                const returnType = this.extractReturnType(func.returnType, entityRegistry);
                const methodSignature = `${func.name}_${params.map((p: any) => p.type).join('_')}`;
                if (!addedMethods.has(methodSignature)) {
                    methods.push({
                        name: func.name,
                        returnType,
                        parameters: params,
                        body: this.generateFunctionalityMethodBody(func, returnType, aggregateName),
                        throwsException: false
                    });
                    addedMethods.add(methodSignature);
                }
            });
        }

        return methods;
    }

    private generateCrudMethods(aggregateName: string, lowerAggregate: string, consistencyModels: string[]): any[] {
        const dtoType = `${aggregateName}Dto`;
        const methods: any[] = [];

        methods.push({
            name: `create${aggregateName}`,
            returnType: dtoType,
            parameters: [{ type: dtoType, name: `${lowerAggregate}Dto` }],
            body: this.generateCrudMethodBody('create', aggregateName, lowerAggregate, dtoType, [`${lowerAggregate}Dto`]),
            throwsException: false
        });

        methods.push({
            name: `get${aggregateName}ById`,
            returnType: dtoType,
            parameters: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            body: this.generateCrudMethodBody('getById', aggregateName, lowerAggregate, dtoType, [`${lowerAggregate}AggregateId`]),
            throwsException: false
        });

        methods.push({
            name: `update${aggregateName}`,
            returnType: dtoType,
            parameters: [
                { type: 'Integer', name: `${lowerAggregate}AggregateId` },
                { type: dtoType, name: `${lowerAggregate}Dto` }
            ],
            body: this.generateCrudMethodBody('update', aggregateName, lowerAggregate, dtoType, [`${lowerAggregate}AggregateId`, `${lowerAggregate}Dto`]),
            throwsException: false
        });

        methods.push({
            name: `delete${aggregateName}`,
            returnType: 'void',
            parameters: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            body: this.generateCrudMethodBody('delete', aggregateName, lowerAggregate, 'void', [`${lowerAggregate}AggregateId`]),
            throwsException: false
        });

        return methods;
    }

    private generateCrudMethodBody(operation: string, aggregateName: string, lowerAggregate: string, returnType: string, paramNames: string[]): string {
        const capitalizedMethodName = this.capitalize(operation === 'getById' ? `get${aggregateName}ById` : `${operation}${aggregateName}`);
        const methodName = operation === 'getById' ? `get${aggregateName}ById` : `${operation}${aggregateName}`;
        const uncapitalizedMethodName = methodName.charAt(0).toLowerCase() + methodName.slice(1);
        const sagaParams = [`${lowerAggregate}Service`, 'sagaUnitOfWorkService', ...paramNames, 'sagaUnitOfWork'].join(', ');

        let sagaReturn: string;
        if (returnType === 'void') {
            sagaReturn = 'break;';
        } else if (operation === 'create') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getCreated${aggregateName}Dto();`;
        } else if (operation === 'getById') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.get${aggregateName}Dto();`;
        } else if (operation === 'update') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getUpdated${aggregateName}Dto();`;
        } else {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getResult();`;
        }

        return `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${capitalizedMethodName}FunctionalitySagas ${uncapitalizedMethodName}FunctionalitySagas = new ${capitalizedMethodName}FunctionalitySagas(
                        ${sagaParams});
                ${uncapitalizedMethodName}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                ${sagaReturn}
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;
    }

    private buildImports(aggregate: Aggregate, rootEntity: Entity | undefined, options: CoordinationGenerationOptions, dependencies: any[], entityRegistry: EntityRegistry, businessMethods: any[]): string[] {
        const imports: string[] = [];
        const projectName = options.projectName.toLowerCase();

        const basePackage = this.getBasePackage();

        imports.push('import java.util.Arrays;');
        imports.push(`import ${basePackage}.${projectName}.microservices.exception.${this.capitalize(options.projectName)}Exception;`);
        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.core.env.Environment;');
        imports.push('import org.springframework.stereotype.Service;');
        imports.push('import jakarta.annotation.PostConstruct;');
        imports.push(`import ${basePackage}.ms.TransactionalModel;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.${projectName}.sagas.coordination.${aggregate.name.toLowerCase()}.*;`);

        const usesUserDto = this.checkUserDtoUsage(aggregate, rootEntity);
        if (usesUserDto) {
            imports.push(`import ${basePackage}.${projectName}.shared.dtos.UserDto;`);
        }

        const usesAggregateState = this.checkAggregateStateUsage(aggregate, rootEntity);
        if (usesAggregateState) {
            imports.push(`import ${basePackage}.ms.domain.aggregate.Aggregate.AggregateState;`);
        }

        dependencies.forEach(dep => {
            if (dep.required && !dep.name.includes('UnitOfWorkService')) {
                const serviceName = dep.name.toLowerCase().replace('service', '');
                imports.push(`import ${basePackage}.${projectName}.microservices.${serviceName}.service.${dep.type};`);
            }
        });

        if (rootEntity) {
            imports.push(`import ${basePackage}.${projectName}.shared.dtos.${rootEntity.name}Dto;`);
        }

        if (aggregate.entities) {
            aggregate.entities.forEach((entity: any) => {
                if (entity.name !== rootEntity?.name) {
                    imports.push(`import ${basePackage}.${projectName}.shared.dtos.${entity.name}Dto;`);
                }
            });
        }

        this.addDtoImports(aggregate, imports, projectName, entityRegistry);

        // Remove duplicate imports
        return Array.from(new Set(imports));
    }

    private checkUserDtoUsage(aggregate: Aggregate, rootEntity: Entity | undefined): boolean {
        if ((aggregate as any).metadata?.requiresUserDto) {
            return true;
        }

        const methodUsage = aggregate.methods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'User' || param.type === 'UserDto') ||
            method.returnType === 'UserDto' || method.returnType === 'User'
        );

        const workflowUsage = aggregate.workflows?.some((workflow: any) =>
            workflow.parameters?.some((param: any) => {
                const paramStr = typeof param === 'string' ? param : (param.type || '');
                return typeof paramStr === 'string' && (paramStr.includes('User') || paramStr.includes('UserDto'));
            })
        );

        const serviceUsage = (aggregate as any).serviceDefinition?.serviceMethods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'UserDto') ||
            method.returnType === 'UserDto'
        );

        const rootEntityUsage = rootEntity?.properties?.some((prop: any) =>
            prop.type === 'User' || prop.type === 'UserDto' ||
            prop.name?.toLowerCase().includes('user')
        );

        const entityUsage = aggregate.entities?.some((entity: any) => {
            const propUsage = entity.properties?.some((prop: any) =>
                prop.type === 'User' || prop.type === 'UserDto' ||
                prop.name?.toLowerCase().includes('user')
            );

            const constructorUsage = entity.constructors?.some((constructor: any) =>
                constructor.parameters?.some((param: any) =>
                    param.type === 'UserDto' || param.type === 'User'
                )
            );

            const methodUsage = entity.methods?.some((method: any) =>
                method.parameters?.some((param: any) => param.type === 'User' || param.type === 'UserDto') ||
                method.returnType === 'UserDto' || method.returnType === 'User'
            );

            return propUsage || constructorUsage || methodUsage;
        });

        return methodUsage || workflowUsage || serviceUsage || rootEntityUsage || entityUsage || false;
    }

    private checkAggregateStateUsage(aggregate: Aggregate, rootEntity: Entity | undefined): boolean {
        if ((aggregate as any).metadata?.requiresAggregateState) {
            return true;
        }

        const methodUsage = aggregate.methods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'AggregateState') ||
            method.returnType === 'AggregateState'
        );

        const workflowUsage = aggregate.workflows?.some((workflow: any) =>
            workflow.parameters?.some((param: any) => {
                const paramStr = typeof param === 'string' ? param : (param.type || '');
                return typeof paramStr === 'string' && paramStr.includes('AggregateState');
            }) ||
            workflow.returnType === 'AggregateState'
        );

        const serviceUsage = (aggregate as any).serviceDefinition?.serviceMethods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'AggregateState') ||
            method.returnType === 'AggregateState'
        );

        const rootEntityUsage = rootEntity?.properties?.some((prop: any) =>
            prop.type === 'AggregateState' || prop.name?.includes('State')
        );

        const entityUsage = aggregate.entities?.some((entity: any) => {
            const propUsage = entity.properties?.some((prop: any) =>
                prop.type === 'AggregateState' ||
                prop.name?.includes('State') ||
                prop.name?.toLowerCase().includes('state')
            );

            const constructorUsage = entity.constructors?.some((constructor: any) =>
                constructor.parameters?.some((param: any) =>
                    param.type === 'AggregateState'
                )
            );

            const methodUsage = entity.methods?.some((method: any) =>
                method.parameters?.some((param: any) => param.type === 'AggregateState') ||
                method.returnType === 'AggregateState'
            );

            return propUsage || constructorUsage || methodUsage;
        });

        return methodUsage || workflowUsage || serviceUsage || rootEntityUsage || entityUsage || false;
    }



    private extractReturnType(returnType: any, entityRegistry: EntityRegistry): string {
        if (!returnType) return 'void';

        if (typeof returnType === 'string') {
            if (entityRegistry.isEntityName(returnType)) {
                return returnType + 'Dto';
            }
            return returnType;
        }

        const type = returnType;

        if (type.$type === 'PrimitiveType') {
            return TypeResolver.resolveJavaType(type);
        } else if (type.$type === 'EntityType' && type.type && type.type.ref) {
            return type.type.ref.name + 'Dto';
        } else if (type.$type === 'BuiltinType') {
            return type.name;
        } else if (type.$type === 'ListType' && type.elementType) {
            const elementType = this.extractReturnType(type.elementType, entityRegistry);
            return `List<${elementType}>`;
        } else if (type.$type === 'SetType' && type.elementType) {
            const elementType = this.extractReturnType(type.elementType, entityRegistry);
            return `Set<${elementType}>`;
        } else if (type.$type === 'CollectionType') {
            const elementType = this.extractReturnType(type.elementType, entityRegistry);
            return `List<${elementType}>`;
        } else if (type.name) {
            if (entityRegistry.isEntityName(type.name)) {
                return type.name + 'Dto';
            }
            return type.name;
        }

        return 'void';
    }


    private addDtoImports(aggregate: Aggregate, imports: string[], projectName: string, entityRegistry: EntityRegistry): void {
        const usedDtoTypes = new Set<string>();

        if (aggregate.methods) {
            aggregate.methods.forEach((method: any) => {
                const returnType = this.extractReturnType(method.returnType, entityRegistry);
                this.collectDtoTypesFromReturnType(returnType, usedDtoTypes);
            });
        }

        if (aggregate.workflows) {
            aggregate.workflows.forEach((workflow: any) => {
                const returnType = this.extractReturnType(workflow.returnType, entityRegistry);
                this.collectDtoTypesFromReturnType(returnType, usedDtoTypes);
            });
        }

        const dtoPackage = `${this.getBasePackage()}.${projectName}.shared.dtos`;
        usedDtoTypes.forEach(dtoType => {
            imports.push(`import ${dtoPackage}.${dtoType};`);
        });
    }

    private collectDtoTypesFromReturnType(returnType: string, usedDtoTypes: Set<string>): void {
        if (returnType.includes('Dto')) {
            const match = returnType.match(/(\w+Dto)/g);
            if (match) {
                match.forEach(dto => usedDtoTypes.add(dto));
            }
        }
    }


    private extractEndpointParameters(parameters: any): any[] {
        if (!parameters) return [];

        return parameters.map((param: any) => {
            let paramType = 'Object';
            const paramName = param.name || 'param';

            if (param.type) {
                const type = param.type;

                if (typeof type === 'string') {
                    paramType = type;
                } else if (type.$type === 'PrimitiveType') {
                    paramType = TypeResolver.resolveJavaType(type);
                } else if (type.$type === 'EntityType' && type.type && type.type.ref) {
                    paramType = type.type.ref.name + 'Dto';
                } else if (type.$type === 'BuiltinType') {
                    paramType = type.name;
                } else if (type.name) {
                    paramType = type.name;
                }
            }

            return { type: paramType, name: paramName, annotation: param.annotation };
        });
    }

    private extractFunctionalityParameters(parameters: any): any[] {
        if (!parameters) return [];
        return parameters.map((param: any) => {
            if (typeof param === 'string') {
                const parts = param.trim().split(/\s+/);
                if (parts.length >= 2) {
                    return { type: parts[0], name: parts[1] };
                }
                return { type: 'Object', name: param };
            }
            const paramName = param.name || 'param';
            let paramType = 'Object';
            if (param.type) {
                const type = param.type;
                if (typeof type === 'string') {
                    paramType = type;
                } else if (type.$type === 'PrimitiveType') {
                    paramType = TypeResolver.resolveJavaType(type);
                } else if (type.$type === 'EntityType' && type.type && type.type.ref) {
                    paramType = type.type.ref.name + 'Dto';
                } else if (type.$type === 'BuiltinType') {
                    paramType = type.name;
                } else if ((type as any).typeName || (type as any).name) {
                    paramType = (type as any).typeName || (type as any).name;
                }
            }
            return { type: paramType, name: paramName };
        });
    }

    private generateWebApiMethodBody(endpoint: any, returnType: string, aggregateName: string, consistencyModels: string[]): string {
        const methodName = endpoint.methodName;
        const capitalizedMethodName = this.capitalize(methodName);
        const lowerAggregateName = aggregateName.toLowerCase();
        const params = this.extractEndpointParameters(endpoint.parameters);

        const sagaParams = this.buildSagaParameters(params, lowerAggregateName);
        const sagaCall = this.buildSagaCall(methodName, returnType);

        const cases = `            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${capitalizedMethodName}FunctionalitySagas ${methodName}FunctionalitySagas = new ${capitalizedMethodName}FunctionalitySagas(
                        ${sagaParams});
                ${methodName}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                ${sagaCall}`;

        let body = `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
${cases}
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;

        return body;
    }

    private generateFunctionalityMethodBody(func: any, returnType: string, aggregateName: string): string {
        const methodName = func.name;
        const capitalizedMethodName = this.capitalize(methodName);
        const lowerAggregateName = aggregateName.toLowerCase();
        const params = this.extractFunctionalityParameters(func.parameters);
        const paramNames = params.map(p => p.name).join(', ');

        const body = `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${capitalizedMethodName}FunctionalitySagas ${methodName}FunctionalitySagas = new ${capitalizedMethodName}FunctionalitySagas(${lowerAggregateName}Service, sagaUnitOfWorkService, sagaUnitOfWork);
                ${methodName}FunctionalitySagas.buildWorkflow(${paramNames});
                ${methodName}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                ${returnType !== 'void' ? `return ${methodName}FunctionalitySagas.getResult();` : ''}
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;
        return body;
    }


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

    private buildSagaParameters(params: any[], aggregateName: string): string {
        const baseParams = [`${aggregateName}Service`, 'sagaUnitOfWorkService'];

        params.forEach(param => {
            if (param.name) {
                baseParams.push(param.name);
            }
        });

        baseParams.push('sagaUnitOfWork');
        return baseParams.join(', ');
    }

    private buildSagaCall(methodName: string, returnType: string): string {
        if (returnType === 'void') {
            return 'break;';
        } else if (returnType.startsWith('List<')) {
            return `return ${methodName}FunctionalitySagas.get${this.capitalize(methodName.replace('get', ''))}();`;
        } else if (returnType.startsWith('Set<')) {
            return `return ${methodName}FunctionalitySagas.get${this.capitalize(methodName.replace('get', ''))}();`;
        } else if (returnType.includes('Dto')) {
            if (methodName.startsWith('create')) {
                return `return ${methodName}FunctionalitySagas.getCreated${this.capitalize(methodName.replace('create', ''))}();`;
            } else if (methodName.startsWith('get')) {
                return `return ${methodName}FunctionalitySagas.get${this.capitalize(methodName.replace('get', ''))}();`;
            } else {
                return `return ${methodName}FunctionalitySagas.getResult();`;
            }
        } else {
            return `return ${methodName}FunctionalitySagas.getResult();`;
        }
    }


}
