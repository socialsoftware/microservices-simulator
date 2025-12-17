import { Entity } from '../common/parsers/model-parser.js';
import { OrchestrationBase } from '../common/orchestration-base.js';
import { TypeResolver } from '../common/resolvers/type-resolver.js';

export class FunctionalitiesCrudGenerator extends OrchestrationBase {
    generateCrudMethods(aggregateName: string, lowerAggregate: string, rootEntity: Entity): any[] {
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

        const searchableProperties = this.getSearchableProperties(rootEntity);
        if (searchableProperties.length > 0) {
            const searchParams = searchableProperties.map(prop => ({
                type: prop.type,
                name: prop.name
            }));

            methods.push({
                name: `search${aggregateName}s`,
                returnType: `List<${dtoType}>`,
                parameters: searchParams,
                body: this.generateCrudMethodBody('search', aggregateName, lowerAggregate, `List<${dtoType}>`, searchableProperties.map(p => p.name)),
                throwsException: false
            });
        }

        return methods;
    }

    private getSearchableProperties(entity: Entity): { name: string; type: string }[] {
        if (!entity.properties) return [];

        const searchableTypes = ['String', 'Boolean'];
        const properties: { name: string; type: string }[] = [];

        for (const prop of entity.properties) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            let isEnum = false;
            if (propType && typeof propType === 'object' && propType.$type === 'EntityType' && propType.type) {
                const ref = propType.type.ref;
                if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                    isEnum = true;
                } else if (propType.type.$refText) {
                    const javaType = TypeResolver.resolveJavaType(prop.type);
                    if (!TypeResolver.isPrimitiveType(javaType) && !TypeResolver.isEntityType(javaType) &&
                        !javaType.startsWith('List<') && !javaType.startsWith('Set<')) {
                        isEnum = true;
                    }
                }
            }

            if (searchableTypes.includes(typeName) || isEnum) {
                let javaType = TypeResolver.resolveJavaType(prop.type);
                if (javaType === 'boolean') {
                    javaType = 'Boolean';
                }
                properties.push({
                    name: prop.name,
                    type: javaType
                });
            }
        }

        for (const prop of entity.properties) {
            const typeNode: any = (prop as any).type;
            if (!typeNode || typeNode.$type !== 'EntityType' || !typeNode.type) continue;

            const refEntity = typeNode.type.ref as any;
            if (!refEntity || !refEntity.properties) continue;

            for (const relProp of refEntity.properties as any[]) {
                if (!relProp.name || !relProp.name.endsWith('AggregateId')) continue;

                const relType = relProp.type;
                const relTypeName = relType?.typeName || relType?.type?.$refText || relType?.$refText || '';
                if (relTypeName !== 'Integer' && relTypeName !== 'Long') continue;

                properties.push({
                    name: relProp.name,
                    type: relTypeName
                });
            }
        }

        return properties;
    }

    private generateCrudMethodBody(operation: string, aggregateName: string, lowerAggregate: string, returnType: string, paramNames: string[]): string {
        const capitalizedMethodName = this.capitalize(operation === 'getById' ? `get${aggregateName}ById` : operation === 'getAll' ? `getAll${aggregateName}s` : operation === 'search' ? `search${aggregateName}s` : `${operation}${aggregateName}`);
        const methodName = operation === 'getById' ? `get${aggregateName}ById` : operation === 'getAll' ? `getAll${aggregateName}s` : operation === 'search' ? `search${aggregateName}s` : `${operation}${aggregateName}`;
        const uncapitalizedMethodName = methodName.charAt(0).toLowerCase() + methodName.slice(1);
        const sagaParams = [`${lowerAggregate}Service`, 'sagaUnitOfWorkService', ...paramNames, 'sagaUnitOfWork'].join(', ');

        let sagaReturn: string;
        if (returnType === 'void') {
            sagaReturn = 'break;';
        } else if (operation === 'create') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getCreated${aggregateName}Dto();`;
        } else if (operation === 'getAll') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.get${aggregateName}s();`;
        } else if (operation === 'search') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getSearched${aggregateName}Dtos();`;
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
}


