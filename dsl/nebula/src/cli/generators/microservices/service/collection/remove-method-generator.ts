import { Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";

/**
 * Remove Method Generator
 *
 * Generates collection remove methods for removing a single element from a collection.
 * Uses immutable aggregate pattern and publishes RemovedEvent.
 */
export class RemoveMethodGenerator {
    /**
     * Generate remove method for a collection property.
     *
     * Generated method signature:
     * ```java
     * public void removeElement(Integer entityId, Integer identifierField, UnitOfWork unitOfWork)
     * ```
     *
     * Pattern:
     * 1. Load aggregate (old version)
     * 2. Create immutable copy (new version)
     * 3. Remove element from collection by identifier
     * 4. Register changed aggregate
     * 5. Publish RemovedEvent
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
        const capitalizedIdentifier = capitalize(collection.identifierField);

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
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error removing ${collection.singularName}: " + e.getMessage());
        }
    }`;
    }
}
