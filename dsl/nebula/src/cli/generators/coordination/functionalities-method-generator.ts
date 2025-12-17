import { TypeResolver } from '../common/resolvers/type-resolver.js';
import { EntityRegistry } from '../common/utils/entity-registry.js';
import { OrchestrationBase } from '../common/orchestration-base.js';

/**
 * Generates method bodies for functionalities classes
 */
export class FunctionalitiesMethodGenerator extends OrchestrationBase {

    /**
     * Generate web API method body
     */
    generateWebApiMethodBody(endpoint: any, returnType: string, aggregateName: string, consistencyModels: string[]): string {
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

        return `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
${cases}
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;
    }

    /**
     * Generate functionality method body
     */
    generateFunctionalityMethodBody(func: any, returnType: string, aggregateName: string): string {
        const methodName = func.name;
        const capitalizedMethodName = this.capitalize(methodName);
        const lowerAggregateName = aggregateName.toLowerCase();
        const params = this.extractFunctionalityParameters(func.parameters);
        const paramNames = params.map(p => p.name).join(', ');

        return `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${capitalizedMethodName}FunctionalitySagas ${methodName}FunctionalitySagas = new ${capitalizedMethodName}FunctionalitySagas(${lowerAggregateName}Service, sagaUnitOfWorkService, sagaUnitOfWork);
                ${methodName}FunctionalitySagas.buildWorkflow(${paramNames});
                ${methodName}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                ${returnType !== 'void' ? `return ${methodName}FunctionalitySagas.getResult();` : ''}
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;
    }

    /**
     * Extract parameters from endpoint definition
     */
    extractEndpointParameters(parameters: any): any[] {
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

    /**
     * Extract parameters from functionality definition
     */
    extractFunctionalityParameters(parameters: any): any[] {
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

    /**
     * Extract return type from AST node
     */
    extractReturnType(returnType: any, entityRegistry: EntityRegistry): string {
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

    /**
     * Build saga constructor parameters string
     */
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

    /**
     * Build saga result retrieval call
     */
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

