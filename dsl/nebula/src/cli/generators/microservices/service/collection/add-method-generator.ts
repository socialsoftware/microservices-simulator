import { Entity } from "../../../../../language/generated/ast.js";
import { GeneratorBase } from "../../../common/base/generator-base.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";

/**
 * Add Method Generator
 *
 * Generates collection add methods for adding a single element to a collection.
 * Uses immutable aggregate pattern (create from existing, modify, register changed).
 *
 * Extends GeneratorBase for common utilities (capitalize, etc.)
 */
export class AddMethodGenerator extends GeneratorBase {
    /**
     * Generate add method for a collection property.
     *
     * Generated method signature:
     * ```java
     * public ElementDto addElement(Integer entityId, Integer identifierField, ElementDto elementDto, UnitOfWork unitOfWork)
     * ```
     *
     * Pattern:
     * 1. Load aggregate (old version)
     * 2. Create immutable copy (new version)
     * 3. Create element from DTO
     * 4. Add to collection
     * 5. Register changed aggregate
     * 6. Return DTO
     *
     * @param collection Collection metadata
     * @param aggregateName Aggregate name
     * @param rootEntity Root entity
     * @param projectName Project name for exception handling
     * @returns Java method code
     */
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
