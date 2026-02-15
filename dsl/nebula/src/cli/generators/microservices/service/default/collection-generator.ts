import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { CollectionMetadataExtractor } from "../collection/collection-metadata-extractor.js";
import { AddMethodGenerator } from "../collection/add-method-generator.js";
import { AddBatchMethodGenerator } from "../collection/add-batch-method-generator.js";
import { GetMethodGenerator } from "../collection/get-method-generator.js";
import { RemoveMethodGenerator } from "../collection/remove-method-generator.js";
import { UpdateMethodGenerator } from "../collection/update-method-generator.js";



export class ServiceCollectionGenerator {
    

    static generateCollectionMethods(aggregateName: string, rootEntity: Entity, projectName: string, aggregate?: Aggregate): string {
        if (!rootEntity.properties || !aggregate) {
            return '';
        }

        const methods: string[] = [];
        const collections = CollectionMetadataExtractor.findCollectionProperties(rootEntity, aggregate);

        
        const addGenerator = new AddMethodGenerator();
        const addBatchGenerator = new AddBatchMethodGenerator();
        const getGenerator = new GetMethodGenerator();
        const removeGenerator = new RemoveMethodGenerator();
        const updateGenerator = new UpdateMethodGenerator();

        for (const collection of collections) {
            
            methods.push(addGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(addBatchGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(getGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(removeGenerator.generate(collection, aggregateName, rootEntity, projectName));
            methods.push(updateGenerator.generate(collection, aggregateName, rootEntity, projectName, aggregate));
        }

        return methods.join('\n\n');
    }
}
