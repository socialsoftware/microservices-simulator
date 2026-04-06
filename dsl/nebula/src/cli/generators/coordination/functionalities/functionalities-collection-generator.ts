import { Entity, Aggregate } from '../../common/parsers/model-parser.js';
import { CollectionMetadata, CollectionMetadataBuilder } from '../../common/utils/collection-metadata-builder.js';
import { StringUtils } from '../../../utils/string-utils.js';

export interface CollectionFunctionalityMethod {
    name: string;
    returnType: string;
    parameters: Array<{ type: string; name: string }>;
    body: string;
    throwsException: boolean;
    operation: 'add' | 'addBatch' | 'get' | 'update' | 'remove';
}

export class FunctionalitiesCollectionGenerator {
    

    generateCollectionMethods(
        aggregateName: string,
        lowerAggregate: string,
        rootEntity: Entity,
        aggregate: Aggregate,
        projectName?: string
    ): CollectionFunctionalityMethod[] {
        const methods: CollectionFunctionalityMethod[] = [];
        const collections = CollectionMetadataBuilder.extractCollections(aggregate, rootEntity);

        for (const collection of collections) {
            methods.push(...this.generateMethodsForCollection(collection, aggregateName, lowerAggregate, projectName));
        }

        return methods;
    }

    

    private generateMethodsForCollection(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string,
        projectName?: string
    ): CollectionFunctionalityMethod[] {
        return [
            this.generateAddMethod(collection, aggregateName, lowerAggregate, projectName),
            this.generateAddBatchMethod(collection, aggregateName, lowerAggregate, projectName),
            this.generateGetMethod(collection, aggregateName, lowerAggregate, projectName),
            this.generateUpdateMethod(collection, aggregateName, lowerAggregate, projectName),
            this.generateRemoveMethod(collection, aggregateName, lowerAggregate, projectName)
        ];
    }

    

    private generateAddMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string,
        projectName?: string
    ): CollectionFunctionalityMethod {
        const methodName = `add${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;
        const uncapitalizedMethod = methodName.charAt(0).toLowerCase() + methodName.slice(1);

        const parameters = [
            { type: 'Integer', name: `${lowerAggregate}Id` },
            { type: collection.identifierType, name: collection.identifierField },
            { type: collection.elementDtoType, name: `${collection.singularName}Dto` }
        ];

        const body = `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${sagaClassName} ${uncapitalizedMethod}FunctionalitySagas = new ${sagaClassName}(
                        sagaUnitOfWorkService,
                        ${lowerAggregate}Id, ${collection.identifierField}, ${collection.singularName}Dto,
                        sagaUnitOfWork, commandGateway);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.getAdded${collection.capitalizedSingular}Dto();
            default: throw new ${StringUtils.capitalize(projectName || 'answers')}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;

        return {
            name: methodName,
            returnType: collection.elementDtoType,
            parameters,
            body,
            throwsException: false,
            operation: 'add'
        };
    }



    private generateAddBatchMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string,
        projectName?: string
    ): CollectionFunctionalityMethod {
        const methodName = `add${aggregateName}${collection.capitalizedSingular}s`;
        const sagaClassName = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;
        const uncapitalizedMethod = methodName.charAt(0).toLowerCase() + methodName.slice(1);

        const parameters = [
            { type: 'Integer', name: `${lowerAggregate}Id` },
            { type: `List<${collection.elementDtoType}>`, name: `${collection.singularName}Dtos` }
        ];

        const body = `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${sagaClassName} ${uncapitalizedMethod}FunctionalitySagas = new ${sagaClassName}(
                        sagaUnitOfWorkService,
                        ${lowerAggregate}Id, ${collection.singularName}Dtos,
                        sagaUnitOfWork, commandGateway);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.getAdded${collection.capitalizedSingular}Dtos();
            default: throw new ${StringUtils.capitalize(projectName || 'answers')}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;

        return {
            name: methodName,
            returnType: `List<${collection.elementDtoType}>`,
            parameters,
            body,
            throwsException: false,
            operation: 'addBatch'
        };
    }



    private generateGetMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string,
        projectName?: string
    ): CollectionFunctionalityMethod {
        const methodName = `get${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;
        const uncapitalizedMethod = methodName.charAt(0).toLowerCase() + methodName.slice(1);

        const parameters = [
            { type: 'Integer', name: `${lowerAggregate}Id` },
            { type: collection.identifierType, name: collection.identifierField }
        ];

        const body = `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${sagaClassName} ${uncapitalizedMethod}FunctionalitySagas = new ${sagaClassName}(
                        sagaUnitOfWorkService,
                        ${lowerAggregate}Id, ${collection.identifierField},
                        sagaUnitOfWork, commandGateway);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.get${collection.capitalizedSingular}Dto();
            default: throw new ${StringUtils.capitalize(projectName || 'answers')}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;

        return {
            name: methodName,
            returnType: collection.elementDtoType,
            parameters,
            body,
            throwsException: false,
            operation: 'get'
        };
    }



    private generateUpdateMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string,
        projectName?: string
    ): CollectionFunctionalityMethod {
        const methodName = `update${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;
        const uncapitalizedMethod = methodName.charAt(0).toLowerCase() + methodName.slice(1);

        const parameters = [
            { type: 'Integer', name: `${lowerAggregate}Id` },
            { type: collection.identifierType, name: collection.identifierField },
            { type: collection.elementDtoType, name: `${collection.singularName}Dto` }
        ];

        const body = `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${sagaClassName} ${uncapitalizedMethod}FunctionalitySagas = new ${sagaClassName}(
                        sagaUnitOfWorkService,
                        ${lowerAggregate}Id, ${collection.identifierField}, ${collection.singularName}Dto,
                        sagaUnitOfWork, commandGateway);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.getUpdated${collection.capitalizedSingular}Dto();
            default: throw new ${StringUtils.capitalize(projectName || 'answers')}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;

        return {
            name: methodName,
            returnType: collection.elementDtoType,
            parameters,
            body,
            throwsException: false,
            operation: 'update'
        };
    }



    private generateRemoveMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string,
        projectName?: string
    ): CollectionFunctionalityMethod {
        const methodName = `remove${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;
        const uncapitalizedMethod = methodName.charAt(0).toLowerCase() + methodName.slice(1);

        const parameters = [
            { type: 'Integer', name: `${lowerAggregate}Id` },
            { type: collection.identifierType, name: collection.identifierField }
        ];

        const body = `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${sagaClassName} ${uncapitalizedMethod}FunctionalitySagas = new ${sagaClassName}(
                        sagaUnitOfWorkService,
                        ${lowerAggregate}Id, ${collection.identifierField},
                        sagaUnitOfWork, commandGateway);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ${StringUtils.capitalize(projectName || 'answers')}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;

        return {
            name: methodName,
            returnType: 'void',
            parameters,
            body,
            throwsException: false,
            operation: 'remove'
        };
    }
}
