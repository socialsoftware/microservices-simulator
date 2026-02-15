import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, PublishedEventContext } from "./event-types.js";
import { EXTENDED_PRIMITIVE_TYPES } from "../../common/utils/type-constants.js";
import { getEffectiveFieldMappings } from "../../../utils/aggregate-helpers.js";

export class PublishedEventGenerator extends EventBaseGenerator {
    async generatePublishedEvents(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};
        const context = this.buildPublishedEventsContext(aggregate, rootEntity, options);

        for (const event of context.publishedEvents) {
            const eventContext = {
                ...context,
                event
            };
            // Use fullEventName to avoid key collisions between root and projection entity events
            const eventKey = event.fullEventName || event.capitalizedEventName || event.eventName;
            results[`published-event-${eventKey}`] = await this.generateIndividualPublishedEvent(eventContext);
        }

        return results;
    }

    private buildPublishedEventsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): PublishedEventContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const rootEntityEvents = this.buildPublishedEvents(rootEntity, baseContext.aggregateName);
        const projectionEntityEvents = this.buildProjectionEntityEvents(aggregate, baseContext.aggregateName);
        const collectionManipulationEvents = this.buildCollectionManipulationEvents(aggregate, rootEntity);
        const publishedEvents = [...rootEntityEvents, ...projectionEntityEvents, ...collectionManipulationEvents];
        const imports = this.buildPublishedEventsImports(aggregate, options, publishedEvents);

        return {
            ...baseContext,
            publishedEvents,
            imports
        };
    }

    private buildPublishedEvents(rootEntity: Entity, aggregateName: string): any[] {
        const eventTypes = ['Updated', 'Deleted'];

        return eventTypes.map(eventType => {
            const variations = this.getEventNameVariations(eventType, aggregateName);
            let properties: any[] = [];

            if (eventType === 'Updated') {
                const allProperties = this.buildEventProperties(rootEntity, eventType);
                const rootProps = (rootEntity.properties || []) as any[];

                properties = allProperties.filter((prop: any) => {
                    const rootProp = rootProps.find(p => p.name === prop.name);
                    const typeNode = rootProp?.type;

                    let isEntityRef = false;
                    let isCollectionRef = false;
                    if (typeNode && typeof typeNode === 'object') {
                        const t: any = typeNode;
                        if (t.$type === 'EntityType') {
                            isEntityRef = true;
                        } else if (t.$type === 'ListType' || t.$type === 'SetType') {
                            isCollectionRef = true;
                        }
                    }

                    return (
                        !prop.isFinal &&
                        !prop.isCollection &&
                        !prop.isEntity &&
                        !isEntityRef &&
                        !isCollectionRef
                    );
                });
            } else if (eventType === 'Deleted') {
                properties = [];
            }

            return {
                eventType,
                ...variations,
                properties,
                timestamp: new Date().toISOString()
            };
        });
    }

    private buildProjectionEntityEvents(aggregate: Aggregate, aggregateName: string): any[] {
        const projectionEvents: any[] = [];

        // Find all projection entities (non-root entities with uses clause)
        const projectionEntities = (aggregate.entities || []).filter((e: any) =>
            !e.isRoot && e.aggregateRef
        );

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;
            const sourceAggregateName = (projectionEntity as any).aggregateRef;
            const prefix = sourceAggregateName.toLowerCase();

            // Generate DeletedEvent for projection entity
            const deletedEventVariations = this.getEventNameVariations('Deleted', projectionEntityName);
            const deletedFields = [
                {
                    name: `${prefix}AggregateId`,
                    type: 'Integer',
                    capitalizedName: this.capitalize(`${prefix}AggregateId`),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                }
            ];

            projectionEvents.push({
                eventType: 'Deleted',
                ...deletedEventVariations,
                properties: deletedFields,
                isProjectionEvent: true,
                projectionEntityName,
                sourceAggregateName,
                timestamp: new Date().toISOString()
            });

            // Generate UpdatedEvent for projection entity
            const updatedEventVariations = this.getEventNameVariations('Updated', projectionEntityName);
            const updatedFields = [
                {
                    name: `${prefix}AggregateId`,
                    type: 'Integer',
                    capitalizedName: this.capitalize(`${prefix}AggregateId`),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                },
                {
                    name: `${prefix}Version`,
                    type: 'Integer',
                    capitalizedName: this.capitalize(`${prefix}Version`),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                }
            ];

            // Add primitive mapped fields from projection entity
            const primitiveFields = this.extractPrimitiveProjectionFields(projectionEntity, prefix);
            updatedFields.push(...primitiveFields);

            projectionEvents.push({
                eventType: 'Updated',
                ...updatedEventVariations,
                properties: updatedFields,
                isProjectionEvent: true,
                projectionEntityName,
                sourceAggregateName,
                timestamp: new Date().toISOString()
            });
        }

        return projectionEvents;
    }

    /**
     * Build RemovedEvent and UpdatedEvent for collection element types
     */
    private buildCollectionManipulationEvents(aggregate: Aggregate, rootEntity: Entity): any[] {
        const collectionEvents: any[] = [];

        if (!rootEntity.properties) {
            return collectionEvents;
        }

        // Find all collection properties
        for (const prop of rootEntity.properties) {
            const propType = (prop as any).type;

            // Check if this is a collection type (Set or List)
            if (propType && (propType.$type === 'SetType' || propType.$type === 'ListType')) {
                const elementType = propType.elementType?.type?.ref?.name || propType.elementType?.type?.$refText;

                if (!elementType) continue;

                // Find the element entity
                const elementEntity = aggregate.entities?.find((e: any) => e.name === elementType);
                if (!elementEntity) continue;

                // Check if it's a projection entity or dto entity
                const isProjection = (elementEntity as any).aggregateRef !== undefined;

                // Determine identifier field
                let identifierField: string;
                if (isProjection) {
                    // Projection entity: use {prefix}AggregateId
                    const referencedAggregateName = this.extractReferencedAggregateName(elementType);
                    identifierField = `${referencedAggregateName.toLowerCase()}AggregateId`;
                } else {
                    // Dto entity: use business key
                    identifierField = this.determineBusinessKey(elementEntity);
                }

                // Generate RemovedEvent
                const removedEventVariations = this.getEventNameVariations('Removed', elementType);
                collectionEvents.push({
                    eventType: 'Removed',
                    ...removedEventVariations,
                    properties: [
                        {
                            name: identifierField,
                            type: 'Integer',
                            capitalizedName: this.capitalize(identifierField),
                            isFinal: false,
                            isCollection: false,
                            isEntity: false
                        }
                    ],
                    isCollectionEvent: true,
                    elementType,
                    timestamp: new Date().toISOString()
                });

                // Generate UpdatedEvent - but SKIP for projection entities
                // (projection entities already have UpdatedEvent from buildProjectionEntityEvents)
                if (!isProjection) {
                    // Generate UpdatedEvent with ONLY identifier field (simplified approach)
                    // Collection manipulation events should be minimal - just track what changed, not all the data
                    const updatedEventVariations = this.getEventNameVariations('Updated', elementType);
                    collectionEvents.push({
                        eventType: 'Updated',
                        ...updatedEventVariations,
                        properties: [
                            {
                                name: identifierField,
                                type: 'Integer',
                                capitalizedName: this.capitalize(identifierField),
                                isFinal: false,
                                isCollection: false,
                                isEntity: false
                            }
                            // NO additional updatable fields - keep events minimal
                        ],
                        isCollectionEvent: true,
                        elementType,
                        timestamp: new Date().toISOString()
                    });
                }
            }
        }

        return collectionEvents;
    }

    /**
     * Extract referenced aggregate name from projection entity name
     * ExecutionUser -> User, AnswerQuestion -> Question
     */
    private extractReferencedAggregateName(entityName: string): string {
        // Find the last capital letter that starts a new word
        for (let i = entityName.length - 1; i >= 0; i--) {
            if (entityName[i] === entityName[i].toUpperCase() && i > 0) {
                const candidate = entityName.substring(i);
                if (candidate.length > 1) {
                    return candidate;
                }
            }
        }
        return entityName;
    }

    /**
     * Determine business key field for dto entity
     */
    private determineBusinessKey(entity: any): string {
        if (!entity || !entity.properties) {
            return 'key';
        }

        // Look for common business key field names
        const commonKeys = ['key', 'code', 'id', 'sequence'];

        for (const keyName of commonKeys) {
            const field = entity.properties.find((p: any) => p.name === keyName);
            if (field) {
                return keyName;
            }
        }

        // Fallback: first Integer field that's not aggregateId/version
        const firstIntField = entity.properties.find((p: any) => {
            const javaType = this.resolveJavaType(p.type);
            return javaType === 'Integer' &&
                !p.name.endsWith('AggregateId') &&
                !p.name.endsWith('Version');
        });

        return firstIntField?.name || 'key';
    }

    private extractPrimitiveProjectionFields(entity: any, prefix: string): any[] {
        const fields: any[] = [];

        // Check fieldMappings for projection entities (those with 'uses' clause)
        // Use getEffectiveFieldMappings to get field mappings with resolved types
        const fieldMappings = getEffectiveFieldMappings(entity as Entity);

        for (const mapping of fieldMappings) {
            const fieldName = mapping.entityField;

            // Skip system fields that are already included
            if (fieldName === 'id' || fieldName.endsWith('AggregateId') ||
                fieldName.endsWith('Version') || fieldName.endsWith('State')) {
                continue;
            }

            // Get the Java type for this field mapping
            const javaType = this.resolveJavaType(mapping.type);

            // Skip collections and entity references
            if (javaType.startsWith('Set<') || javaType.startsWith('List<') ||
                this.isEntityType(mapping.type)) {
                continue;
            }

            // Include primitive mapped fields
            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                fields.push({
                    name: fieldName,
                    type: javaType,
                    capitalizedName: this.capitalize(fieldName),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                });
            }
        }

        // Also check regular properties (for explicitly declared fields)
        for (const prop of entity.properties || []) {
            const propName = prop.name;

            // Skip system fields that are already included
            if (propName === 'id' || propName.endsWith('AggregateId') ||
                propName.endsWith('Version') || propName.endsWith('State')) {
                continue;
            }

            // Get the Java type for this property
            const javaType = this.resolveJavaType(prop.type);

            // Skip collections and entity references
            if (javaType.startsWith('Set<') || javaType.startsWith('List<') ||
                this.isEntityType(prop.type)) {
                continue;
            }

            // Include primitive fields
            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                fields.push({
                    name: propName,
                    type: javaType,
                    capitalizedName: this.capitalize(propName),
                    isFinal: prop.isFinal || false,
                    isCollection: false,
                    isEntity: false
                });
            }
        }

        return fields;
    }

    private buildPublishedEventsImports(aggregate: Aggregate, options: EventGenerationOptions, publishedEvents: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);

        return [
            ...baseImports,
            'import java.time.LocalDateTime;',
            'import java.io.Serializable;',
            'import com.fasterxml.jackson.annotation.JsonFormat;',
            ''
        ];
    }

    private async generateIndividualPublishedEvent(context: any): Promise<string> {
        const template = this.loadRawTemplate('events/published-event.hbs');
        const propertyImports: string[] = [];
        const addedImports = new Set<string>();

        if (context.event.properties && context.event.properties.length > 0) {
            context.event.properties.forEach((prop: any) => {
                if (prop.isEntity && prop.type) {
                    const entityPackage = this.generateEventPackageName(
                        context.basePackage || 'unknown',
                        context.projectName || 'unknown',
                        prop.referencedAggregateName || context.aggregateName || 'unknown',
                        'shared',
                        'dtos'
                    );
                    const importStmt = `import ${entityPackage}.${prop.type};`;
                    if (!addedImports.has(importStmt)) {
                        propertyImports.push(importStmt);
                        addedImports.add(importStmt);
                    }
                } else if (prop.isEnum && prop.enumType) {
                    const basePackage = context.basePackage || 'unknown';
                    const enumPackage = `${basePackage}.${(context.projectName || 'unknown').toLowerCase()}.shared.enums`;
                    const importStmt = `import ${enumPackage}.${prop.enumType};`;
                    if (!addedImports.has(importStmt)) {
                        propertyImports.push(importStmt);
                        addedImports.add(importStmt);
                    }
                } else if (prop.type) {
                    // Check for built-in types that require imports
                    const type = prop.type;
                    if (type === 'LocalDateTime' || type.includes('LocalDateTime')) {
                        const importStmt = 'import java.time.LocalDateTime;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    } else if (type === 'BigDecimal' || type.includes('BigDecimal')) {
                        const importStmt = 'import java.math.BigDecimal;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    } else if (type.startsWith('List<')) {
                        const importStmt = 'import java.util.List;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    } else if (type.startsWith('Set<')) {
                        const importStmt = 'import java.util.Set;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    }
                }
            });
        }

        const templateContext = {
            packageName: context.packageName,
            eventName: context.event.fullEventName,
            fields: context.event.properties || [],
            imports: propertyImports.length > 0 ? propertyImports.join('\n') : undefined
        };
        return this.renderTemplateFromString(template, templateContext);
    }
}
