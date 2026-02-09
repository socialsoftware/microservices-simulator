import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { CollectionMetadataBuilder } from "../../common/utils/collection-metadata-builder.js";
import { CollectionEndpoint, CollectionEndpointBuilder } from "../utils/collection-endpoint-builder.js";

export class ControllerCollectionGenerator {
    /**
     * Generate all collection endpoints for an aggregate
     */
    generateCollectionEndpoints(
        aggregate: Aggregate,
        rootEntity: Entity,
        aggregateName: string,
        lowerAggregate: string
    ): CollectionEndpoint[] {
        const endpoints: CollectionEndpoint[] = [];
        const collections = CollectionMetadataBuilder.extractCollections(aggregate, rootEntity);

        for (const collection of collections) {
            const collectionEndpoints = CollectionEndpointBuilder.buildEndpoints(
                collection,
                aggregateName,
                lowerAggregate
            );
            endpoints.push(...collectionEndpoints);
        }

        return endpoints;
    }

    /**
     * Extract DTO types from collection endpoints for imports
     */
    extractDtoTypes(endpoints: CollectionEndpoint[]): Set<string> {
        const dtoTypes = new Set<string>();

        for (const endpoint of endpoints) {
            // Extract from return type
            if (endpoint.returnType) {
                const matches = endpoint.returnType.match(/(\w+Dto)/g);
                if (matches) {
                    matches.forEach(dto => dtoTypes.add(dto));
                }
            }

            // Extract from parameters
            for (const param of endpoint.parameters) {
                const matches = param.type.match(/(\w+Dto)/g);
                if (matches) {
                    matches.forEach(dto => dtoTypes.add(dto));
                }
            }
        }

        return dtoTypes;
    }

    /**
     * Check if collection endpoints need List/Set imports
     */
    needsCollectionImports(endpoints: CollectionEndpoint[]): { needsList: boolean; needsSet: boolean } {
        let needsList = false;
        let needsSet = false;

        for (const endpoint of endpoints) {
            if (endpoint.returnType?.includes('List<')) needsList = true;
            if (endpoint.returnType?.includes('Set<')) needsSet = true;

            for (const param of endpoint.parameters) {
                if (param.type.includes('List<')) needsList = true;
                if (param.type.includes('Set<')) needsSet = true;
            }
        }

        return { needsList, needsSet };
    }

    /**
     * Check if any collection endpoints use HttpStatus annotations
     */
    needsHttpStatusImport(endpoints: CollectionEndpoint[]): boolean {
        return endpoints.some(e => e.responseStatus !== undefined);
    }
}
