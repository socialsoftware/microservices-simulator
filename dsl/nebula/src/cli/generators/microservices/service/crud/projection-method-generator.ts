import { Entity } from "../../../../../language/generated/ast.js";
import { EntityExt, AggregateExt, TypeGuards } from "../../../../types/ast-extensions.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { getEvents, getEntities } from "../../../../utils/aggregate-helpers.js";
import { EventNameParser } from "../../../common/utils/event-name-parser.js";
import { EventHandlerCodeGenerator } from "./event-handler-code-generator.js";

/**
 * Generates projection update methods for subscribed events.
 *
 * Responsibilities:
 * - Generate event handler methods for subscribed CRUD events
 * - Handle simple subscriptions (no conditions, no routing)
 * - Identify projection entities that need updating
 * - Delegate to EventHandlerCodeGenerator for actual code generation
 */
export class ProjectionMethodGenerator {
    /**
     * Generate event handler methods for subscribed CRUD events.
     *
     * These methods handle events by updating/marking as INACTIVE projections when events are received.
     * Only handles simple subscriptions (Update/Delete events without conditions or routing).
     *
     * @param aggregateName Name of the aggregate
     * @param aggregate The aggregate definition
     * @param projectName Project name for exception handling
     * @returns Generated Java methods as a string, or empty string if none
     */
    static generateProjectionMethods(aggregateName: string, aggregate: AggregateExt, projectName: string): string {
        const events = getEvents(aggregate);
        if (!events || !events.subscribedEvents) {
            return '';
        }

        // Filter for simple subscriptions (Update/Delete events)
        const simpleSubscriptions = events.subscribedEvents.filter((sub: any) => {
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        if (simpleSubscriptions.length === 0) {
            return '';
        }

        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntity = aggregate.entities.find((e: Entity) => TypeGuards.isRootEntity(e as EntityExt));
        if (!rootEntity) {
            return '';
        }

        const methods: string[] = [];

        for (const subscription of simpleSubscriptions) {
            const eventTypeName = (subscription as any).eventType || '';
            if (!eventTypeName) continue;

            const isUpdate = eventTypeName.includes('Updated');
            const isDelete = eventTypeName.includes('Deleted');

            if (!isUpdate && !isDelete) continue;

            // Extract publisher aggregate name (e.g., UserDeletedEvent -> User)
            let publisherAggregateName = EventNameParser.extractEntityName(eventTypeName);

            // Find ALL entities in this aggregate that use the publisher aggregate
            const entities = getEntities(aggregate);
            let projectionEntities = entities.filter((e: any) => {
                const aggregateRef = e.aggregateRef;
                return aggregateRef && aggregateRef.toLowerCase() === publisherAggregateName.toLowerCase();
            });

            // If no entities found, this might be a projection entity event (e.g., ExecutionUserUpdatedEvent)
            // Try to find entities whose aggregateRef name appears in the event name
            if (projectionEntities.length === 0) {
                projectionEntities = entities.filter((e: any) => {
                    const aggregateRef = e.aggregateRef;
                    // Check if event name contains the aggregateRef (e.g., "User" in "ExecutionUserUpdatedEvent")
                    return aggregateRef && eventTypeName.toLowerCase().includes(aggregateRef.toLowerCase());
                });

                // Update publisherAggregateName to the actual source aggregate if we found matches
                if (projectionEntities.length > 0) {
                    const publisherAggregateNameFromRef = (projectionEntities[0] as any).aggregateRef;
                    if (publisherAggregateNameFromRef) {
                        // Use the source aggregate name from the projection entity's aggregateRef
                        publisherAggregateName = publisherAggregateNameFromRef;
                    }
                }
            }

            // Always generate handler methods, even if no projection entities exist
            // If no projections, generates a stub handler for custom business logic

            // Generate a single event handler method that handles all matching projections (or stub if none)
            if (isDelete) {
                methods.push(EventHandlerCodeGenerator.generateEventHandlerMethod(
                    capitalizedAggregate,
                    lowerAggregate,
                    rootEntity,
                    projectionEntities,
                    publisherAggregateName,
                    eventTypeName,
                    'delete',
                    projectName
                ));
            } else if (isUpdate) {
                methods.push(EventHandlerCodeGenerator.generateEventHandlerMethod(
                    capitalizedAggregate,
                    lowerAggregate,
                    rootEntity,
                    projectionEntities,
                    publisherAggregateName,
                    eventTypeName,
                    'update',
                    projectName
                ));
            }
        }

        return methods.length > 0 ? '\n' + methods.join('\n\n') : '';
    }
}
