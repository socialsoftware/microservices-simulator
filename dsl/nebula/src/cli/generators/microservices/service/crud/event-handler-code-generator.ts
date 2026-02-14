import { Entity } from "../../../../../language/generated/ast.js";
import { EntityExt } from "../../../../types/ast-extensions.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { getEntities, getAllModels, getAggregateRefName, getEffectiveFieldMappings } from "../../../../utils/aggregate-helpers.js";
import { EXTENDED_PRIMITIVE_TYPES } from "../../../common/utils/type-constants.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";

/**
 * Generates event handler method code for projection entity updates.
 *
 * Responsibilities:
 * - Generate event handler methods for subscribed CRUD events
 * - Handle projection entity updates (mark as INACTIVE on delete, update version on update)
 * - Extract primitive fields from publisher aggregate events
 * - Register projection entity events
 */
export class EventHandlerCodeGenerator {
    /**
     * Generate a single event handler method that handles all matching projections.
     *
     * This method:
     * 1. Loads the aggregate
     * 2. Creates immutable copy
     * 3. Updates projection entities (mark INACTIVE or update version)
     * 4. Registers projection entity events
     * 5. Saves changes
     *
     * @param capitalizedAggregate Capitalized aggregate name
     * @param lowerAggregate Lowercase aggregate name
     * @param rootEntity The root entity
     * @param projectionEntities Projection entities that reference the publisher aggregate
     * @param publisherAggregateName Name of the aggregate publishing the event
     * @param eventTypeName Full event type name (e.g., "UserDeletedEvent")
     * @param action Whether this is a delete or update event
     * @param projectName Project name for exception handling
     * @returns Generated Java method code
     */
    static generateEventHandlerMethod(
        capitalizedAggregate: string,
        lowerAggregate: string,
        rootEntity: EntityExt,
        projectionEntities: EntityExt[],
        publisherAggregateName: string,
        eventTypeName: string,
        action: 'delete' | 'update',
        projectName: string
    ): string {
        const rootEntityName = rootEntity.name;
        const methodName = `handle${eventTypeName}`;

        // Build the method body
        const projectionUpdates: string[] = [];

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;

            // Extract the field prefix from the projection entity
            // Look for the aggregateId field to determine the prefix
            // E.g., TournamentCreator has "creatorAggregateId" -> prefix is "creator"
            const aggregateIdField = (projectionEntity.properties || []).find((p: any) =>
                p.name && p.name.toLowerCase() === `${publisherAggregateName.toLowerCase()}aggregateid`
            );

            // Skip this projection entity if it doesn't have a matching aggregateId field
            // E.g., AnswerExecution (from Execution) shouldn't be updated by ExecutionUserUpdatedEvent
            if (!aggregateIdField) {
                continue;
            }

            // Extract prefix from field like "creatorAggregateId" -> "creator"
            const fieldPrefix = (aggregateIdField as any).name.replace(/AggregateId$/, '');
            const capitalizedFieldPrefix = capitalize(fieldPrefix);

            // Find properties in root entity containing this projection
            const matchingProperties = rootEntity.properties?.filter(prop => {
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const elementType = TypeResolver.getElementType(prop.type);
                return javaType.includes(projectionEntityName) || elementType === projectionEntityName;
            }) || [];

            for (const prop of matchingProperties) {
                const isCollection = TypeResolver.resolveJavaType(prop.type).startsWith('Set<') ||
                                   TypeResolver.resolveJavaType(prop.type).startsWith('List<');
                const propName = prop.name;

                if (isCollection) {
                    // For collections: find all matching entities and mark as INACTIVE
                    if (action === 'delete') {
                        projectionUpdates.push(
                            `        // Handle ${propName} collection\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().stream()\n` +
                            `                .filter(item -> item.get${capitalizedFieldPrefix}AggregateId() != null && \n` +
                            `                               item.get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId))\n` +
                            `                .forEach(item -> item.set${capitalizedFieldPrefix}State(Aggregate.AggregateState.INACTIVE));\n` +
                            `        }`
                        );
                    } else if (action === 'update') {
                        projectionUpdates.push(
                            `        // Handle ${propName} collection\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().stream()\n` +
                            `                .filter(item -> item.get${capitalizedFieldPrefix}AggregateId() != null && \n` +
                            `                               item.get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId))\n` +
                            `                .forEach(item -> item.set${capitalizedFieldPrefix}Version(${publisherAggregateName.toLowerCase()}Version));\n` +
                            `        }`
                        );
                    }
                } else {
                    // For single entity: check and update/mark as INACTIVE
                    if (action === 'delete') {
                        projectionUpdates.push(
                            `        // Handle ${propName} single reference\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null && \n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId() != null &&\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId)) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().set${capitalizedFieldPrefix}State(Aggregate.AggregateState.INACTIVE);\n` +
                            `        }`
                        );
                    } else if (action === 'update') {
                        projectionUpdates.push(
                            `        // Handle ${propName} single reference\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null && \n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId() != null &&\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId)) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().set${capitalizedFieldPrefix}Version(${publisherAggregateName.toLowerCase()}Version);\n` +
                            `        }`
                        );
                    }
                }
            }
        }

        const projectionUpdateCode = projectionUpdates.join('\n\n');

        // Generate event registrations for projection entities
        const eventRegistrations: string[] = [];

        // Extract primitive fields from the publisher aggregate's UpdatedEvent
        // We need to pass these to the projection entity UpdatedEvent
        const primitiveFieldParams = this.extractPrimitiveFieldsForEvent(projectionEntities, publisherAggregateName);

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;

            if (action === 'delete') {
                const prefix = publisherAggregateName.toLowerCase();
                // Register DeletedEvent for projection entity
                eventRegistrations.push(
                    `        unitOfWorkService.registerEvent(\n` +
                    `            new ${projectionEntityName}DeletedEvent(\n` +
                    `                new${rootEntityName}.getAggregateId(),\n` +
                    `                ${prefix}AggregateId\n` +
                    `            ),\n` +
                    `            unitOfWork\n` +
                    `        );`
                );
            } else if (action === 'update') {
                // Check if projection entity has local properties (properties not mapped from source aggregate)
                const hasLocalProperties = (projectionEntity.properties || []).some((prop: any) => {
                    const propName = prop.name;
                    // Skip ONLY the base system fields (exact matches)
                    if (propName === 'id' || propName === 'aggregateId' ||
                        propName === 'version' || propName === 'state') {
                        return false;
                    }
                    // Check if this property is from fieldMappings
                    const fieldMappings = (projectionEntity as any).fieldMappings || [];
                    const isFromMapping = fieldMappings.some((m: any) => m.entityField === propName || m.dtoField === propName);
                    return !isFromMapping; // Has local property if not from mapping
                });

                // Only register UpdatedEvent if projection entity has NO local properties
                // (because we can't populate local properties from the incoming event)
                if (!hasLocalProperties) {
                    const prefix = publisherAggregateName.toLowerCase();
                    // Build the parameter list for the UpdatedEvent constructor
                    // Format: aggregateId, {prefix}AggregateId, {prefix}Version, ...primitive fields
                    const eventParams = [
                        `new${rootEntityName}.getAggregateId()`,
                        `${prefix}AggregateId`,
                        `${prefix}Version`,
                        ...primitiveFieldParams.paramList
                    ].join(',\n                    ');

                    eventRegistrations.push(
                        `        unitOfWorkService.registerEvent(\n` +
                        `            new ${projectionEntityName}UpdatedEvent(\n` +
                        `                    ${eventParams}\n` +
                        `            ),\n` +
                        `            unitOfWork\n` +
                        `        );`
                    );
                }
            }
        }

        const eventRegistrationCode = eventRegistrations.length > 0
            ? '\n' + eventRegistrations.join('\n\n')
            : '';

        // Build method parameter list including primitive fields for update action
        const methodParamList = action === 'update'
            ? `Integer aggregateId, Integer ${publisherAggregateName.toLowerCase()}AggregateId, Integer ${publisherAggregateName.toLowerCase()}Version${primitiveFieldParams.methodSignature}`
            : `Integer aggregateId, Integer ${publisherAggregateName.toLowerCase()}AggregateId, Integer ${publisherAggregateName.toLowerCase()}Version`;

        const methodBody = `            ${rootEntityName} old${rootEntityName} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            ${rootEntityName} new${rootEntityName} = ${lowerAggregate}Factory.create${rootEntityName}FromExisting(old${rootEntityName});

${projectionUpdateCode}

            unitOfWorkService.registerChanged(new${rootEntityName}, unitOfWork);
${eventRegistrationCode}

            return new${rootEntityName};`;

        return `    public ${rootEntityName} ${methodName}(${methodParamList}, UnitOfWork unitOfWork) {
${ExceptionGenerator.generateTryCatchWrapper(projectName, `handling ${eventTypeName}`, lowerAggregate, methodBody)}
    }`;
    }

    /**
     * Extract primitive field parameters needed for projection entity UpdatedEvent.
     *
     * Analyzes the projection entity's field mappings to determine which primitive fields
     * from the publisher aggregate need to be passed to the projection entity's UpdatedEvent.
     *
     * @param projectionEntities The projection entities
     * @param publisherAggregateName Name of the publishing aggregate
     * @returns Method signature fragment and parameter list for event constructor
     */
    private static extractPrimitiveFieldsForEvent(projectionEntities: EntityExt[], publisherAggregateName: string): {
        methodSignature: string;
        paramList: string[];
    } {
        if (projectionEntities.length === 0) {
            return { methodSignature: '', paramList: [] };
        }

        // Get the source entity that publishes the event
        // For projection chains (e.g., TournamentCreator from ExecutionUser from User),
        // we need to extract fields from the immediate source (ExecutionUser), not the consuming entity
        const projectionEntity = projectionEntities[0];
        const sourceEntityName = getAggregateRefName(projectionEntity as Entity);

        // Find the source entity in the model registry
        let sourceEntity: Entity | null = null;
        if (sourceEntityName) {
            const allModels = getAllModels();
            for (const model of allModels) {
                for (const aggregate of model.aggregates) {
                    const entities = getEntities(aggregate);
                    const found = entities.find((e: any) => e.name === sourceEntityName);
                    if (found) {
                        sourceEntity = found as Entity;
                        break;
                    }
                }
                if (sourceEntity) break;
            }
        }

        // Use the source entity if found, otherwise fall back to the projection entity
        const entityToExtractFrom = sourceEntity || (projectionEntity as Entity);

        // Use getEffectiveFieldMappings to get field mappings with resolved types
        const fieldMappings = getEffectiveFieldMappings(entityToExtractFrom);

        const fields: Array<{ javaType: string; paramName: string }> = [];

        // ONLY extract from fieldMappings (mapped properties from referenced aggregate)
        // DO NOT extract local properties - those don't exist in the incoming event!
        for (const mapping of fieldMappings) {
            const entityField = mapping.entityField;

            // Skip system fields
            if (entityField.endsWith('AggregateId') || entityField.endsWith('Version') || entityField.endsWith('State')) {
                continue;
            }

            const javaType = TypeResolver.resolveJavaType(mapping.type);

            // Only include primitive fields (use === for exact match or includes for partial match)
            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType === t || javaType.includes(t))) {
                // Use the entity field name as the parameter name (matches published event getters)
                fields.push({
                    javaType,
                    paramName: entityField
                });
            }
        }

        // Build method signature: ", Type param1, Type param2"
        const methodSignature = fields.length > 0
            ? ', ' + fields.map(f => `${f.javaType} ${f.paramName}`).join(', ')
            : '';

        // Build param list for constructor call: ["param1", "param2"]
        const paramList = fields.map(f => f.paramName);

        return { methodSignature, paramList };
    }
}
