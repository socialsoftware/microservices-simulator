import { Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";

/**
 * Add Batch Method Generator
 *
 * Generates collection add batch methods for adding multiple elements at once.
 * Uses immutable aggregate pattern with forEach to add each element.
 */
export class AddBatchMethodGenerator {
    /**
     * Generate add batch method for a collection property.
     *
     * Generated method signature:
     * ```java
     * public List<ElementDto> addElements(Integer entityId, List<ElementDto> elementDtos, UnitOfWork unitOfWork)
     * ```
     *
     * Pattern:
     * 1. Load aggregate (old version)
     * 2. Create immutable copy (new version)
     * 3. For each DTO: create element and add to collection
     * 4. Register changed aggregate
     * 5. Return DTOs
     *
     * @param collection Collection metadata
     * @param aggregateName Aggregate name
     * @param rootEntity Root entity
     * @param projectName Project name for exception handling
     * @returns Java method code
     */
    static generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();

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
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error adding ${collection.singularName}s: " + e.getMessage());
        }
    }`;
    }
}
