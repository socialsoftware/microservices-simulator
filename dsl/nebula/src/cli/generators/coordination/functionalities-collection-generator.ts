import { Entity, Aggregate } from '../common/parsers/model-parser.js';
import { CollectionMetadata, CollectionMetadataBuilder } from '../common/utils/collection-metadata-builder.js';

export interface CollectionFunctionalityMethod {
    name: string;
    returnType: string;
    parameters: Array<{ type: string; name: string }>;
    body: string;
    throwsException: boolean;
    operation: 'add' | 'addBatch' | 'get' | 'update' | 'remove';
}

export class FunctionalitiesCollectionGenerator {
    // Helper method migrated from OrchestrationBase
    private capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
    /**
     * Generate collection operation methods for functionalities layer
     */
    generateCollectionMethods(
        aggregateName: string,
        lowerAggregate: string,
        rootEntity: Entity,
        aggregate: Aggregate
    ): CollectionFunctionalityMethod[] {
        const methods: CollectionFunctionalityMethod[] = [];
        const collections = CollectionMetadataBuilder.extractCollections(aggregate, rootEntity);

        for (const collection of collections) {
            methods.push(...this.generateMethodsForCollection(collection, aggregateName, lowerAggregate));
        }

        return methods;
    }

    /**
     * Generate all 5 methods for a single collection
     */
    private generateMethodsForCollection(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionFunctionalityMethod[] {
        return [
            this.generateAddMethod(collection, aggregateName, lowerAggregate),
            this.generateAddBatchMethod(collection, aggregateName, lowerAggregate),
            this.generateGetMethod(collection, aggregateName, lowerAggregate),
            this.generateUpdateMethod(collection, aggregateName, lowerAggregate),
            this.generateRemoveMethod(collection, aggregateName, lowerAggregate)
        ];
    }

    /**
     * Generate add single element method
     */
    private generateAddMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionFunctionalityMethod {
        const methodName = `add${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${this.capitalize(methodName)}FunctionalitySagas`;
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
                        sagaUnitOfWork, sagaUnitOfWorkService, ${lowerAggregate}Service,
                        ${lowerAggregate}Id, ${collection.identifierField}, ${collection.singularName}Dto);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.getAdded${collection.capitalizedSingular}Dto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
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

    /**
     * Generate add batch method
     */
    private generateAddBatchMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionFunctionalityMethod {
        const methodName = `add${aggregateName}${collection.capitalizedSingular}s`;
        const sagaClassName = `${this.capitalize(methodName)}FunctionalitySagas`;
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
                        sagaUnitOfWork, sagaUnitOfWorkService, ${lowerAggregate}Service,
                        ${lowerAggregate}Id, ${collection.singularName}Dtos);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.getAdded${collection.capitalizedSingular}Dtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
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

    /**
     * Generate get single element method
     */
    private generateGetMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionFunctionalityMethod {
        const methodName = `get${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${this.capitalize(methodName)}FunctionalitySagas`;
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
                        sagaUnitOfWork, sagaUnitOfWorkService, ${lowerAggregate}Service,
                        ${lowerAggregate}Id, ${collection.identifierField});
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.get${collection.capitalizedSingular}Dto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
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

    /**
     * Generate update element method
     */
    private generateUpdateMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionFunctionalityMethod {
        const methodName = `update${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${this.capitalize(methodName)}FunctionalitySagas`;
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
                        sagaUnitOfWork, sagaUnitOfWorkService, ${lowerAggregate}Service,
                        ${lowerAggregate}Id, ${collection.identifierField}, ${collection.singularName}Dto);
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return ${uncapitalizedMethod}FunctionalitySagas.getUpdated${collection.capitalizedSingular}Dto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
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

    /**
     * Generate remove element method
     */
    private generateRemoveMethod(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionFunctionalityMethod {
        const methodName = `remove${aggregateName}${collection.capitalizedSingular}`;
        const sagaClassName = `${this.capitalize(methodName)}FunctionalitySagas`;
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
                        sagaUnitOfWork, sagaUnitOfWorkService, ${lowerAggregate}Service,
                        ${lowerAggregate}Id, ${collection.identifierField});
                ${uncapitalizedMethod}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
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
