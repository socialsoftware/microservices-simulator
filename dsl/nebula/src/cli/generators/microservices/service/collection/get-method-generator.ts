import { Entity } from "../../../../../language/generated/ast.js";
import { GeneratorBase } from "../../../common/base/generator-base.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";



export class GetMethodGenerator extends GeneratorBase {
    

    generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = this.lowercase(entityName);
        const capitalizedIdentifier = this.capitalize(collection.identifierField);

        return `    public ${collection.elementType}Dto get${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${collection.elementType} element = ${lowerEntity}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${this.capitalize(projectName)}Exception("${collection.elementType} not found"));
            return element.buildDto();
${ExceptionGenerator.generateCatchBlock(projectName, 'retrieving', collection.singularName)}
    }`;
    }
}
