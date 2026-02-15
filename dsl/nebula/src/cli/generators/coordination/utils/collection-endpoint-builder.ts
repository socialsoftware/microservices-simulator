import { CollectionMetadata } from "../../common/utils/collection-metadata-builder.js";

export interface EndpointParameter {
    name: string;
    type: string;
    annotation: string;
}

export interface CollectionEndpoint {
    method: 'Post' | 'Get' | 'Put' | 'Delete';
    path: string;
    methodName: string;
    parameters: EndpointParameter[];
    returnType: string | null;
    description: string;
    operation: 'add' | 'addBatch' | 'get' | 'update' | 'remove';
    isCollection: true;
    responseStatus?: string;
}

export class CollectionEndpointBuilder {
    

    static buildEndpoints(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionEndpoint[] {
        return [
            this.buildAddEndpoint(collection, aggregateName, lowerAggregate),
            this.buildAddBatchEndpoint(collection, aggregateName, lowerAggregate),
            this.buildGetEndpoint(collection, aggregateName, lowerAggregate),
            this.buildUpdateEndpoint(collection, aggregateName, lowerAggregate),
            this.buildRemoveEndpoint(collection, aggregateName, lowerAggregate)
        ];
    }

    

    private static buildAddEndpoint(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionEndpoint {
        return {
            method: 'Post',
            path: `/${lowerAggregate}s/{${lowerAggregate}Id}/${collection.propertyName}`,
            methodName: `add${aggregateName}${collection.capitalizedSingular}`,
            parameters: [
                {
                    name: `${lowerAggregate}Id`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                },
                {
                    name: collection.identifierField,
                    type: collection.identifierType,
                    annotation: '@RequestParam'
                },
                {
                    name: `${collection.singularName}Dto`,
                    type: collection.elementDtoType,
                    annotation: '@RequestBody'
                }
            ],
            returnType: collection.elementDtoType,
            description: `Add ${collection.singularName} to ${aggregateName}`,
            operation: 'add',
            isCollection: true,
            responseStatus: '@ResponseStatus(HttpStatus.CREATED)'
        };
    }

    

    private static buildAddBatchEndpoint(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionEndpoint {
        return {
            method: 'Post',
            path: `/${lowerAggregate}s/{${lowerAggregate}Id}/${collection.propertyName}/batch`,
            methodName: `add${aggregateName}${collection.capitalizedSingular}s`,
            parameters: [
                {
                    name: `${lowerAggregate}Id`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                },
                {
                    name: `${collection.singularName}Dtos`,
                    type: `List<${collection.elementDtoType}>`,
                    annotation: '@RequestBody'
                }
            ],
            returnType: `List<${collection.elementDtoType}>`,
            description: `Add multiple ${collection.propertyName} to ${aggregateName}`,
            operation: 'addBatch',
            isCollection: true
        };
    }

    

    private static buildGetEndpoint(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionEndpoint {
        return {
            method: 'Get',
            path: `/${lowerAggregate}s/{${lowerAggregate}Id}/${collection.propertyName}/{${collection.identifierField}}`,
            methodName: `get${aggregateName}${collection.capitalizedSingular}`,
            parameters: [
                {
                    name: `${lowerAggregate}Id`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                },
                {
                    name: collection.identifierField,
                    type: collection.identifierType,
                    annotation: '@PathVariable'
                }
            ],
            returnType: collection.elementDtoType,
            description: `Get ${collection.singularName} from ${aggregateName}`,
            operation: 'get',
            isCollection: true
        };
    }

    

    private static buildUpdateEndpoint(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionEndpoint {
        return {
            method: 'Put',
            path: `/${lowerAggregate}s/{${lowerAggregate}Id}/${collection.propertyName}/{${collection.identifierField}}`,
            methodName: `update${aggregateName}${collection.capitalizedSingular}`,
            parameters: [
                {
                    name: `${lowerAggregate}Id`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                },
                {
                    name: collection.identifierField,
                    type: collection.identifierType,
                    annotation: '@PathVariable'
                },
                {
                    name: `${collection.singularName}Dto`,
                    type: collection.elementDtoType,
                    annotation: '@RequestBody'
                }
            ],
            returnType: collection.elementDtoType,
            description: `Update ${collection.singularName} in ${aggregateName}`,
            operation: 'update',
            isCollection: true
        };
    }

    

    private static buildRemoveEndpoint(
        collection: CollectionMetadata,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionEndpoint {
        return {
            method: 'Delete',
            path: `/${lowerAggregate}s/{${lowerAggregate}Id}/${collection.propertyName}/{${collection.identifierField}}`,
            methodName: `remove${aggregateName}${collection.capitalizedSingular}`,
            parameters: [
                {
                    name: `${lowerAggregate}Id`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                },
                {
                    name: collection.identifierField,
                    type: collection.identifierType,
                    annotation: '@PathVariable'
                }
            ],
            returnType: null,
            description: `Remove ${collection.singularName} from ${aggregateName}`,
            operation: 'remove',
            isCollection: true,
            responseStatus: '@ResponseStatus(HttpStatus.NO_CONTENT)'
        };
    }
}
