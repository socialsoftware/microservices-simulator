import { Entity } from "../../../../../language/generated/ast.js";
import { GeneratorBase } from "../../../common/base/generator-base.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";



export class AddMethodGenerator extends GeneratorBase {
    

    generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = this.lowercase(entityName);
        const lowerAggregate = this.lowercase(aggregateName);

        const body = `            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.elementType} element = new ${collection.elementType}(${collection.singularName}Dto);
            new${entityName}.get${collection.capitalizedCollection}().add(element);
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            return ${collection.singularName}Dto;`;

        const catchBlock = ExceptionGenerator.generateCatchBlock(projectName, 'adding', collection.singularName);

        return `    public ${collection.elementType}Dto add${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
${body}
${catchBlock}
    }`;
    }
}
