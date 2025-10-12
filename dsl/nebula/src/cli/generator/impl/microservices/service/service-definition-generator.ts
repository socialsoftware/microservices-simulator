import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../../base/orchestration-base.js";
import { getGlobalConfig } from "../../../base/config.js";

export interface ServiceGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export class ServiceDefinitionGenerator extends OrchestrationBase {
    async generateServiceFromDefinition(aggregate: Aggregate, rootEntity: Entity, options: ServiceGenerationOptions): Promise<string> {
        const serviceDefinition = (aggregate as any).serviceDefinition;
        if (!serviceDefinition) {
            return this.generateDefaultService(aggregate, rootEntity, options);
        }

        const context = this.buildServiceContext(aggregate, rootEntity, serviceDefinition, options);
        const template = this.loadTemplate('service/service-definition.hbs');
        return this.renderTemplate(template, context);
    }

    private buildServiceContext(aggregate: Aggregate, rootEntity: Entity, serviceDefinition: any, options: ServiceGenerationOptions): any {
        const aggregateName = aggregate.name;
        const serviceName = serviceDefinition.name || `${aggregateName}Service`;
        const packageName = `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.service`;

        const imports = this.buildServiceImports(aggregate, serviceDefinition, options);
        const methods = this.buildServiceMethods(serviceDefinition, aggregate, rootEntity);
        const dependencies = this.buildServiceDependencies(aggregate, serviceDefinition);

        return {
            packageName,
            serviceName,
            aggregateName,
            lowerAggregate: aggregateName.toLowerCase(),
            rootEntityName: rootEntity.name,
            imports,
            methods,
            dependencies,
            generateCrud: serviceDefinition.generateCrud || false,
            transactional: serviceDefinition.transactional || false,
            projectName: options.projectName.toLowerCase(),
            annotations: this.getFrameworkAnnotations()
        };
    }

    private buildServiceImports(aggregate: Aggregate, serviceDefinition: any, options: ServiceGenerationOptions): string[] {
        const imports = [
            'import org.springframework.stereotype.Service;',
            'import org.springframework.beans.factory.annotation.Autowired;',
            `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'aggregate')}.*;`,
            `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'repository')}.*;`,
            'import java.util.List;',
            'import java.util.Set;',
            'import java.util.Optional;'
        ];

        if (serviceDefinition.transactional) {
            imports.push('import org.springframework.transaction.annotation.Transactional;');
        }

        // Check if any method uses business logic that requires additional imports
        const hasBusinessLogic = serviceDefinition.serviceMethods?.some((method: any) =>
            method.implementation?.actions?.length > 0
        );

        if (hasBusinessLogic) {
            imports.push(
                'import java.sql.SQLException;',
                'import org.springframework.dao.CannotAcquireLockException;',
                'import org.springframework.retry.annotation.Backoff;',
                'import org.springframework.retry.annotation.Retryable;',
                'import org.springframework.transaction.annotation.Isolation;',
                'import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;',
                'import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;',
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', 'exception')}.${this.capitalize(options.projectName)}Exception;`,
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', 'exception')}.${this.capitalize(options.projectName)}ErrorMessage;`
            );
        }

        // Add imports based on method parameters and return types
        const hasUserDto = serviceDefinition.serviceMethods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'UserDto') ||
            method.returnType === 'UserDto'
        );

        const hasAggregateState = serviceDefinition.serviceMethods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'AggregateState') ||
            method.returnType === 'AggregateState'
        );

        if (hasUserDto) {
            imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.user.UserDto;');
        }

        if (hasAggregateState) {
            imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateState;');
        }

        return imports;
    }

    private buildServiceMethods(serviceDefinition: any, aggregate: Aggregate, rootEntity: Entity): any[] {
        const methods: any[] = [];

        if (serviceDefinition.generateCrud) {
            methods.push(...this.generateCrudMethods(aggregate, rootEntity));
        }

        if (serviceDefinition.serviceMethods) {
            serviceDefinition.serviceMethods.forEach((method: any) => {
                const methodImplementation = this.processMethodImplementation(method.implementation, aggregate);

                methods.push({
                    name: method.name,
                    parameters: method.parameters?.map((param: any) => ({
                        type: this.resolveJavaType(param.type || 'Object'),
                        name: param.name || 'param'
                    })) || [],
                    returnType: this.resolveJavaType(method.returnType || 'void'),
                    annotations: method.annotations || [],
                    implementation: methodImplementation
                });
            });
        }

        return methods;
    }

    private generateCrudMethods(aggregate: Aggregate, rootEntity: Entity): any[] {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        return [
            {
                name: `create${entityName}`,
                parameters: [{ type: `${entityName}Dto`, name: `${lowerEntity}Dto` }],
                returnType: entityName,
                annotations: []
            },
            {
                name: `find${entityName}ById`,
                parameters: [{ type: 'Integer', name: 'id' }],
                returnType: `Optional<${entityName}>`,
                annotations: []
            },
            {
                name: `update${entityName}`,
                parameters: [
                    { type: 'Integer', name: 'id' },
                    { type: `${entityName}Dto`, name: `${lowerEntity}Dto` }
                ],
                returnType: entityName,
                annotations: []
            },
            {
                name: `delete${entityName}`,
                parameters: [{ type: 'Integer', name: 'id' }],
                returnType: 'void',
                annotations: []
            },
            {
                name: `findAll${aggregateName}s`,
                parameters: [],
                returnType: `List<${entityName}>`,
                annotations: []
            }
        ];
    }

    private generateDefaultService(aggregate: Aggregate, rootEntity: Entity, options: ServiceGenerationOptions): string {
        const context = {
            packageName: getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'service'),
            serviceName: `${aggregate.name}Service`,
            aggregateName: aggregate.name,
            lowerAggregate: aggregate.name.toLowerCase(),
            rootEntityName: rootEntity.name,
            imports: [
                'import org.springframework.stereotype.Service;',
                'import org.springframework.beans.factory.annotation.Autowired;',
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'aggregate')}.*;`,
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'repository')}.*;`
            ],
            methods: [],
            generateCrud: false,
            transactional: false,
            projectName: options.projectName.toLowerCase()
        };

        const template = this.loadTemplate('service/service-definition.hbs');
        return this.renderTemplate(template, context);
    }

    protected override resolveJavaType(type: string | any): string {
        let typeStr: string;

        if (typeof type === 'string') {
            typeStr = type;
        } else if (type?.$refText) {
            typeStr = type.$refText;
        } else if (type?.typeName) {
            typeStr = type.typeName;
        } else if (type?.type) {
            return this.resolveJavaType(type.type);
        } else {
            typeStr = 'Object';
        }

        const typeMap: { [key: string]: string } = {
            'Integer': 'Integer',
            'Long': 'Long',
            'String': 'String',
            'Boolean': 'Boolean',
            'LocalDateTime': 'LocalDateTime',
            'UnitOfWork': 'UnitOfWork',
            'void': 'void',
            'UserDto': 'UserDto',
            'AggregateState': 'AggregateState'
        };

        if (typeStr.startsWith('List<') && typeStr.endsWith('>')) {
            const innerType = typeStr.slice(5, -1);
            return `List<${this.resolveJavaType(innerType)}>`;
        }

        if (typeStr.startsWith('Set<') && typeStr.endsWith('>')) {
            const innerType = typeStr.slice(4, -1);
            return `Set<${this.resolveJavaType(innerType)}>`;
        }

        return typeMap[typeStr] || typeStr;
    }

    private processMethodImplementation(implementation: any, aggregate: Aggregate): any[] {
        if (!implementation?.actions) {
            return [];
        }

        return implementation.actions.map((action: any) => {
            switch (action.$type) {
                case 'LoadAggregateAction':
                    return {
                        action: 'load',
                        aggregateVar: action.aggregateVar,
                        aggregateType: aggregate.name,
                        aggregateId: action.aggregateId,
                        unitOfWorkVar: 'unitOfWork'
                    };

                case 'ValidateAction':
                    return {
                        action: 'validate',
                        condition: action.condition,
                        exception: action.exception,
                        exceptionParams: action.exceptionParams || []
                    };

                case 'CreateEntityAction':
                    return {
                        action: 'create',
                        entityVar: action.entityVar,
                        entityType: action.entityType,
                        constructorParam: action.constructorParams?.[0] || 'oldExecution'
                    };

                case 'DomainOperationAction':
                    return {
                        action: 'execute',
                        targetVar: action.targetVar,
                        operationChain: this.processOperationChain(action.operationChain)
                    };

                case 'RegisterChangeAction':
                    return {
                        action: 'register',
                        aggregateVar: action.aggregateVar,
                        unitOfWorkVar: action.unitOfWorkVar
                    };

                case 'RegisterEventAction':
                    return {
                        action: 'registerEvent',
                        eventType: action.eventType?.ref?.name || action.eventType?.$refText,
                        eventParams: action.eventParams || [],
                        unitOfWorkVar: action.unitOfWorkVar
                    };

                case 'PublishEventAction':
                    return {
                        action: 'publish',
                        eventType: action.eventType?.ref?.name || action.eventType?.$refText,
                        eventParams: action.eventParams || []
                    };

                case 'ReturnAction':
                    return {
                        action: 'return',
                        returnValue: action.returnValue,
                        returnExpression: action.returnExpression
                    };

                default:
                    return { action: 'unknown', raw: action };
            }
        });
    }

    private buildServiceDependencies(aggregate: Aggregate, serviceDefinition: any): any[] {
        const dependencies = [
            {
                type: `${aggregate.name}Repository`,
                name: `${aggregate.name.toLowerCase()}Repository`
            }
        ];

        // Check if any method uses business logic that requires additional dependencies
        const hasBusinessLogic = serviceDefinition.serviceMethods?.some((method: any) =>
            method.implementation?.actions?.length > 0
        );

        if (hasBusinessLogic) {
            dependencies.push(
                {
                    type: 'UnitOfWorkService',
                    name: 'unitOfWorkService'
                },
                {
                    type: `${aggregate.name}Factory`,
                    name: `${aggregate.name.toLowerCase()}Factory`
                }
            );
        }

        return dependencies;
    }

    private processOperationChain(operationChain: any): string {
        if (!operationChain?.operations) {
            return '';
        }

        return operationChain.operations.map((operation: any) => {
            const methodName = operation.methodName;
            const params = operation.params?.map((param: any) => {
                // Handle special constants
                if (param === 'INACTIVE') {
                    return 'Aggregate.AggregateState.INACTIVE';
                }
                return param;
            }).join(', ') || '';
            return params ? `${methodName}(${params})` : methodName;
        }).join('.');
    }
}
