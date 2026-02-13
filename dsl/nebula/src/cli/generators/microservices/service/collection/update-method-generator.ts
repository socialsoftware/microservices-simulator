import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { EXTENDED_PRIMITIVE_TYPES } from "../../../common/utils/type-constants.js";
import { getEffectiveFieldMappings } from "../../../../utils/aggregate-helpers.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";
import { GeneratorBase } from "../../../common/base/generator-base.js";

/**
 * Update Method Generator
 *
 * Generates collection update methods for updating a single element in a collection.
 * Uses immutable aggregate pattern and publishes UpdatedEvent with all primitive fields.
 */
export class UpdateMethodGenerator extends GeneratorBase {
    /**
     * Generate update method for a collection property.
     *
     * Generated method signature:
     * ```java
     * public ElementDto updateElement(Integer entityId, Integer identifierField, ElementDto elementDto, UnitOfWork unitOfWork)
     * ```
     *
     * Pattern:
     * 1. Load aggregate (old version)
     * 2. Create immutable copy (new version)
     * 3. Find element in collection by identifier
     * 4. Update all updatable primitive fields
     * 5. Register changed aggregate
     * 6. Publish UpdatedEvent (with all primitive fields for projections)
     * 7. Return element DTO
     *
     * @param collection Collection metadata
     * @param aggregateName Aggregate name
     * @param rootEntity Root entity
     * @param projectName Project name for exception handling
     * @param aggregate The full aggregate for entity lookup
     * @returns Java method code
     */
    generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string, aggregate: Aggregate): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedIdentifier = this.capitalize(collection.identifierField);

        // Get updatable fields from the element entity
        const elementEntity = aggregate.entities?.find((e: any) => e.name === collection.elementType);
        const updatableFieldsWithMapping = this.extractUpdatableFieldsWithMapping(elementEntity, collection.isProjection, collection.identifierField);

        const updateFieldsCode = updatableFieldsWithMapping.map(field => {
            // Use DTO field name for getter (local name from mapping)
            const dtoGetterField = this.capitalize(field.dtoFieldName);
            // Use entity field name for setter (prefixed name in entity)
            const entitySetterField = this.capitalize(field.entityFieldName);

            return `            if (${collection.singularName}Dto.get${dtoGetterField}() != null) {
                element.set${entitySetterField}(${collection.singularName}Dto.get${dtoGetterField}());
            }`;
        }).join('\n');

        // Build event constructor parameters
        // For projection entities, events include all primitive fields
        // For non-projection entities, events only include identifier
        let eventConstructorParams: string;
        if (collection.isProjection && elementEntity) {
            // Projection entity: pass all fields (aggregateId, identifier, version, + all primitive fields)
            const allEventParams = this.buildProjectionEventParameters(
                elementEntity,
                lowerEntity,
                collection.identifierField,
                'element'
            );
            eventConstructorParams = allEventParams;
        } else {
            // Non-projection entity: only aggregateId + identifier
            eventConstructorParams = `${lowerEntity}Id, ${collection.identifierField}`;
        }

        return `    public ${collection.elementType}Dto update${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.elementType} element = new${entityName}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${this.capitalize(projectName)}Exception("${collection.elementType} not found"));
${updateFieldsCode}
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            ${collection.elementType}UpdatedEvent event = new ${collection.elementType}UpdatedEvent(${eventConstructorParams});
            event.setPublisherAggregateVersion(new${entityName}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
${ExceptionGenerator.generateCatchBlock(projectName, 'updating', collection.singularName)}
    }`;
    }

    /**
     * Extract updatable fields with both DTO and entity field names for projection entities.
     * This is necessary because DTOs use local names (from dtoField) while entities use prefixed names (entityField).
     */
    private extractUpdatableFieldsWithMapping(entity: any, isProjection: boolean, identifierField: string): Array<{
        entityFieldName: string;
        dtoFieldName: string;
        type: string;
    }> {
        if (!entity || !entity.properties) {
            return [];
        }

        const fields: Array<{ entityFieldName: string; dtoFieldName: string; type: string }> = [];
        const excludedFields = ['aggregateId', 'version', 'state', identifierField];

        // For projection entities with field mappings
        if (isProjection && entity.fieldMappings) {

            for (const mapping of entity.fieldMappings) {
                const entityFieldName = mapping.entityField; // e.g., "userName"
                const dtoFieldName = mapping.dtoField;       // e.g., "name"

                // Skip system fields
                if (entityFieldName.endsWith('AggregateId') ||
                    entityFieldName.endsWith('Version') ||
                    entityFieldName.endsWith('State') ||
                    excludedFields.includes(entityFieldName)) {
                    continue;
                }

                const javaType = TypeResolver.resolveJavaType(mapping.type);

                // Skip collections and entity references
                if (javaType.startsWith('Set<') || javaType.startsWith('List<') ||
                    TypeResolver.isEntityType(javaType)) {
                    continue;
                }

                // Include primitive mapped fields
                if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                    fields.push({
                        entityFieldName: entityFieldName,
                        dtoFieldName: dtoFieldName,
                        type: javaType
                    });
                }
            }
        } else {
            // For non-projection entities, entity field name = DTO field name
            for (const prop of entity.properties) {
                const propName = prop.name;

                // Skip excluded fields
                if (excludedFields.includes(propName)) continue;

                // Skip final fields
                if (prop.isFinal) continue;

                // Get the Java type
                const javaType = TypeResolver.resolveJavaType(prop.type);

                // Skip collection fields
                if (javaType.startsWith('Set<') || javaType.startsWith('List<')) {
                    continue;
                }

                // Skip entity references
                if (TypeResolver.isEntityType(javaType)) {
                    continue;
                }

                fields.push({
                    entityFieldName: propName,
                    dtoFieldName: propName, // Same for non-projection entities
                    type: javaType
                });
            }
        }

        return fields;
    }

    /**
     * Build event constructor parameters for projection entity UpdatedEvents.
     * These events include all primitive fields from the projection entity.
     *
     * Parameter order matches the event constructor:
     * 1. aggregateId (root aggregate ID)
     * 2. {prefix}AggregateId (referenced aggregate ID)
     * 3. {prefix}Version (referenced aggregate version)
     * 4. All other primitive mapped fields
     *
     * @param entity The projection entity
     * @param aggregateVarName Variable name of the root aggregate (e.g., "answer")
     * @param identifierField The identifier field name (e.g., "questionAggregateId")
     * @param elementVarName Variable name of the element object (e.g., "element")
     * @returns Comma-separated parameter list for event constructor
     */
    private buildProjectionEventParameters(
        entity: any,
        aggregateVarName: string,
        identifierField: string,
        elementVarName: string
    ): string {
        const params: string[] = [];

        // 1. aggregateId (root aggregate)
        params.push(`${aggregateVarName}Id`);

        // 2. {prefix}AggregateId (referenced aggregate ID - the identifier)
        params.push(`${elementVarName}.get${this.capitalize(identifierField)}()`);

        // 3. {prefix}Version (referenced aggregate version)
        // Extract prefix from identifierField (e.g., "questionAggregateId" -> "question")
        const prefix = identifierField.replace(/AggregateId$/, '');
        const versionField = `${prefix}Version`;
        params.push(`${elementVarName}.get${this.capitalize(versionField)}()`);

        // 4. All other primitive mapped fields (in order from fieldMappings)
        // For projection entities, we need to use the projection entity's OWN field names,
        // not the source entity's field names. For example:
        // - QuizQuestion has fields: questionTitle, questionContent, questionSequence
        // - Not Question's fields: title, content, creationDate

        // Extract fields from the projection entity itself
        let fieldsToProcess: Array<{fieldName: string, type: any}> = [];

        if ((entity as any).aggregateRef) {
            // Projection entity: use field mappings to get the mapped field names
            const fieldMappings = getEffectiveFieldMappings(entity as Entity);
            fieldsToProcess = fieldMappings.map((m: any) => ({
                fieldName: m.entityField,  // Use the projection entity's field name (e.g., questionTitle)
                type: m.type
            }));
        } else {
            // Root entity: use properties directly
            const properties = (entity as any).properties || [];
            fieldsToProcess = properties.map((prop: any) => ({
                fieldName: prop.name,
                type: prop.type
            }));
        }

        for (const field of fieldsToProcess) {
            const fieldName = field.fieldName;

            // Skip system fields (already included above)
            if (fieldName === 'id' ||
                fieldName.endsWith('AggregateId') ||
                fieldName.endsWith('Version') ||
                fieldName.endsWith('State')) {
                continue;
            }

            // Get Java type
            const javaType = TypeResolver.resolveJavaType(field.type);

            // Skip collections and entity references
            if (javaType.startsWith('Set<') ||
                javaType.startsWith('List<') ||
                TypeResolver.isEntityType(javaType)) {
                continue;
            }

            // Include primitive mapped fields
            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${this.capitalize(fieldName)}()`);
            }
        }

        // Also check regular properties (for local fields not from mappings)
        for (const prop of entity.properties || []) {
            const propName = prop.name;

            // Skip system fields
            if (propName === 'id' ||
                propName.endsWith('AggregateId') ||
                propName.endsWith('Version') ||
                propName.endsWith('State')) {
                continue;
            }

            // Get Java type
            const javaType = TypeResolver.resolveJavaType(prop.type);

            // Skip collections and entity references
            if (javaType.startsWith('Set<') ||
                javaType.startsWith('List<') ||
                TypeResolver.isEntityType(javaType)) {
                continue;
            }

            // Include primitive fields (avoid duplicates from fieldsToProcess)
            const alreadyIncluded = fieldsToProcess.some((m: any) => m.fieldName === propName);
            if (!alreadyIncluded && EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${this.capitalize(propName)}()`);
            }
        }

        return params.join(', ');
    }
}
