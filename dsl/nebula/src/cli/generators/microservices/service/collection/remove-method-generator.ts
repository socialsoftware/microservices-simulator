import { Entity } from "../../../../../language/generated/ast.js";
import { GeneratorBase } from "../../../common/base/generator-base.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";



export class RemoveMethodGenerator extends GeneratorBase {
    

    generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = this.lowercase(entityName);
        const lowerAggregate = this.lowercase(aggregateName);
        const capitalizedIdentifier = this.capitalize(collection.identifierField);

        return `    public void remove${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            new${entityName}.get${collection.capitalizedCollection}().removeIf(item ->
                item.get${capitalizedIdentifier}() != null &&
                item.get${capitalizedIdentifier}().equals(${collection.identifierField})
            );
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            ${collection.elementType}RemovedEvent event = new ${collection.elementType}RemovedEvent(${lowerEntity}Id, ${collection.identifierField});
            event.setPublisherAggregateVersion(new${entityName}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
${ExceptionGenerator.generateCatchBlock(projectName, 'removing', collection.singularName)}
    }`;
    }
}
