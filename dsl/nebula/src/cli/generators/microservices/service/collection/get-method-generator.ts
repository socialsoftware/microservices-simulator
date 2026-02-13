import { Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";

/**
 * Get Method Generator
 *
 * Generates collection get methods for retrieving a single element from a collection.
 * Searches collection by identifier field and returns DTO.
 */
export class GetMethodGenerator {
    /**
     * Generate get method for a collection property.
     *
     * Generated method signature:
     * ```java
     * public ElementDto getElement(Integer entityId, Integer identifierField, UnitOfWork unitOfWork)
     * ```
     *
     * Pattern:
     * 1. Load aggregate (read-only)
     * 2. Stream collection to find element by identifier
     * 3. Throw exception if not found
     * 4. Return element DTO
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
        const capitalizedIdentifier = capitalize(collection.identifierField);

        return `    public ${collection.elementType}Dto get${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${collection.elementType} element = ${lowerEntity}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${collection.elementType} not found"));
            return element.buildDto();
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving ${collection.singularName}: " + e.getMessage());
        }
    }`;
    }
}
