import { Entity } from "../../../../../language/generated/ast.js";
import { GeneratorBase } from "../../../common/base/generator-base.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";



export class AddBatchMethodGenerator extends GeneratorBase {
    

    generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = this.lowercase(entityName);
        const lowerAggregate = this.lowercase(aggregateName);

        return `    public List<${collection.elementType}Dto> add${collection.capitalizedSingular}s(Integer ${lowerEntity}Id, List<${collection.elementType}Dto> ${collection.singularName}Dtos, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.singularName}Dtos.forEach(dto -> {
                ${collection.elementType} element = new ${collection.elementType}(dto);
                new${entityName}.get${collection.capitalizedCollection}().add(element);
            });
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            return ${collection.singularName}Dtos;
${ExceptionGenerator.generateCatchBlock(projectName, 'adding', `${collection.singularName}s`)}
    }`;
    }
}
