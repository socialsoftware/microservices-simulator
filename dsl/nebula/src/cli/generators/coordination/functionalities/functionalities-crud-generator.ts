import { Entity, Aggregate } from '../../common/parsers/model-parser.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { CrudHelpers } from '../../common/crud-helpers.js';

export class FunctionalitiesCrudGenerator {
    generateCrudMethods(aggregateName: string, lowerAggregate: string, rootEntity: Entity, aggregate: Aggregate, allAggregates?: Aggregate[], projectName?: string): any[] {
        const dtoType = `${aggregateName}Dto`;
        const createRequestDtoType = `Create${aggregateName}RequestDto`;
        const methods: any[] = [];

        
        
        
        const crossAggregateRefs = CrudHelpers.findCrossAggregateReferences(rootEntity, aggregate, allAggregates);

        
        const createParameters: any[] = [
            { type: createRequestDtoType, name: `createRequest` }
        ];

        const createParamNames = createParameters.map(p => p.name);

        methods.push({
            name: `create${aggregateName}`,
            returnType: dtoType,
            parameters: createParameters,
            body: this.generateCrudMethodBody('create', aggregateName, lowerAggregate, dtoType, createParamNames, [], crossAggregateRefs, projectName),
            throwsException: false
        });

        methods.push({
            name: `get${aggregateName}ById`,
            returnType: dtoType,
            parameters: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            body: this.generateCrudMethodBody('getById', aggregateName, lowerAggregate, dtoType, [`${lowerAggregate}AggregateId`], [], undefined, projectName),
            throwsException: false
        });

        methods.push({
            name: `update${aggregateName}`,
            returnType: dtoType,
            parameters: [
                { type: dtoType, name: `${lowerAggregate}Dto` }
            ],
            body: this.generateCrudMethodBody('update', aggregateName, lowerAggregate, dtoType, [`${lowerAggregate}Dto`], [], undefined, projectName),
            throwsException: false
        });

        methods.push({
            name: `delete${aggregateName}`,
            returnType: 'void',
            parameters: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            body: this.generateCrudMethodBody('delete', aggregateName, lowerAggregate, 'void', [`${lowerAggregate}AggregateId`], [], undefined, projectName),
            throwsException: false
        });

        
        methods.push({
            name: `getAll${aggregateName}s`,
            returnType: `List<${dtoType}>`,
            parameters: [],
            body: this.generateCrudMethodBody('getAll', aggregateName, lowerAggregate, `List<${dtoType}>`, [], [], undefined, projectName),
            throwsException: false
        });

        return methods;
    }


    

    getRelatedDtoType(rel: { entityType: string; paramName: string; javaType: string; isCollection: boolean }, aggregate: Aggregate, allAggregates?: Aggregate[]): { dtoType: string | null; isFromAnotherAggregate: boolean; relatedAggregateName?: string } {
        const relatedEntity = aggregate.entities?.find((e: any) => e.name === rel.entityType);
        if (!relatedEntity) return { dtoType: null, isFromAnotherAggregate: false };

        const entityAny = relatedEntity as any;

        
        const aggregateRef = entityAny.aggregateRef;
        let dtoTypeName: string | null = null;
        let relatedAggregateName: string | undefined = undefined;

        if (aggregateRef) {
            
            if (typeof aggregateRef === 'string') {
                relatedAggregateName = aggregateRef;
                dtoTypeName = `${aggregateRef}Dto`;
            } else if (aggregateRef.ref?.name) {
                relatedAggregateName = aggregateRef.ref.name;
                dtoTypeName = `${aggregateRef.ref.name}Dto`;
            } else if (aggregateRef.$refText) {
                relatedAggregateName = aggregateRef.$refText;
                dtoTypeName = `${aggregateRef.$refText}Dto`;
            }
        }

        
        if (!dtoTypeName && entityAny.generateDto) {
            dtoTypeName = `${rel.entityType}Dto`;
        }

        
        if (relatedAggregateName && allAggregates) {
            const targetAggregate = allAggregates.find(agg => agg.name === relatedAggregateName);
            if (targetAggregate && targetAggregate.name !== aggregate.name) {
                return {
                    dtoType: dtoTypeName,
                    isFromAnotherAggregate: true,
                    relatedAggregateName: targetAggregate.name
                };
            }
        }

        return {
            dtoType: dtoTypeName,
            isFromAnotherAggregate: false
        };
    }



    private generateCrudMethodBody(operation: string, aggregateName: string, lowerAggregate: string, returnType: string, paramNames: string[], crossAggregateServices: Array<{ serviceName: string; aggregateName: string }> = [], crossAggregateRefs?: Array<{ entityType: string; paramName: string; relatedAggregate: string; relatedDtoType: string; isCollection: boolean }>, projectName?: string): string {
        const capitalizedMethodName = StringUtils.capitalize(operation === 'getById' ? `get${aggregateName}ById` : operation === 'getAll' ? `getAll${aggregateName}s` : `${operation}${aggregateName}`);
        const methodName = operation === 'getById' ? `get${aggregateName}ById` : operation === 'getAll' ? `getAll${aggregateName}s` : `${operation}${aggregateName}`;
        const uncapitalizedMethodName = methodName.charAt(0).toLowerCase() + methodName.slice(1);

        
        
        
        
        
        
        
        
        const sagaParams: string[] = ['sagaUnitOfWork', 'sagaUnitOfWorkService', `${lowerAggregate}Service`];

        
        sagaParams.push(...paramNames);

        const sagaParamsString = sagaParams.join(', ');

        let sagaReturn: string;
        if (returnType === 'void') {
            sagaReturn = 'break;';
        } else if (operation === 'create') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getCreated${aggregateName}Dto();`;
        } else if (operation === 'getAll') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.get${aggregateName}s();`;
        } else if (operation === 'getById') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.get${aggregateName}Dto();`;
        } else if (operation === 'update') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getUpdated${aggregateName}Dto();`;
        } else {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getResult();`;
        }

        
        
        const inputParamName = operation === 'create' ? 'createRequest' : (paramNames.find((p: string) => p.endsWith('Dto')) || `${lowerAggregate}Dto`);
        const checkInputCall = (operation === 'create' || operation === 'update')
            ? `checkInput(${inputParamName});\n                `
            : '';

        return `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${checkInputCall}${capitalizedMethodName}FunctionalitySagas ${uncapitalizedMethodName}FunctionalitySagas = new ${capitalizedMethodName}FunctionalitySagas(
                        ${sagaParamsString});
                ${uncapitalizedMethodName}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                ${sagaReturn}
            default: throw new ${StringUtils.capitalize(projectName || 'answers')}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;
    }
}


