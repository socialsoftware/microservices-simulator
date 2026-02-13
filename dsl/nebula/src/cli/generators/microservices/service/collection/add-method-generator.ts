import { Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";

/**
 * Add Method Generator
 *
 * Generates collection add methods for adding a single element to a collection.
 * Uses immutable aggregate pattern (create from existing, modify, register changed).
 */
export class AddMethodGenerator {
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
    static generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();

        return `    public ${collection.elementType}Dto add${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.elementType} element = new ${collection.elementType}(${collection.singularName}Dto);
            new${entityName}.get${collection.capitalizedCollection}().add(element);
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            return ${collection.singularName}Dto;
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error adding ${collection.singularName}: " + e.getMessage());
        }
    }`;
    }
}
