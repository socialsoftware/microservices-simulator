import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventSubscriptionContext } from "./event-types.js";
import { getEntities } from "../../../utils/aggregate-helpers.js";

export class EventSubscriptionGenerator extends EventBaseGenerator {
    async generateEventSubscriptions(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};
        const context = this.buildEventSubscriptionsContext(aggregate, rootEntity, options);

        for (const subscription of context.eventSubscriptions) {
            const subscriptionContext = {
                ...context,
                subscription
            };
            results[`event-subscription-${subscription.eventType}`] = await this.generateIndividualEventSubscription(subscriptionContext);
        }

        return results;
    }

    private buildEventSubscriptionsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventSubscriptionContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventSubscriptions = this.buildEventSubscriptions(aggregate, rootEntity, baseContext.aggregateName, options);
        const imports = this.buildEventSubscriptionsImports(aggregate, options, eventSubscriptions);

        return {
            ...baseContext,
            eventSubscriptions,
            imports
        };
    }

    private buildEventSubscriptions(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, options: EventGenerationOptions): any[] {
        // Get actual subscribed events from DSL
        const events = (aggregate as any).events;
        if (!events || !events.subscribedEvents) {
            return [];
        }

        // Filter for simple subscriptions (no conditions, no routing)
        const simpleSubscriptions = events.subscribedEvents.filter((sub: any) => {
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        const projectName = (this as any).projectName?.toLowerCase() || 'unknown';
        const basePackage = (this as any).getBasePackage?.() || 'pt.ulisboa.tecnico.socialsoftware';

        return simpleSubscriptions.map((sub: any) => {
            const eventTypeName = sub.eventType?.ref?.name || sub.eventType?.$refText || 'UnknownEvent';

            // Determine source aggregate from published event
            let sourceAggregateName = 'unknown';

            // PRIORITY 1: Try AST reference (works for custom events with explicit references)
            const publishedEvent = sub.eventType?.ref as any;
            const eventsContainer = publishedEvent?.$container as any;
            const sourceAggregate = eventsContainer?.$container as Aggregate | undefined;
            if (sourceAggregate?.name) {
                sourceAggregateName = sourceAggregate.name.toLowerCase();
            } else if (sub.sourceAggregate) {
                sourceAggregateName = sub.sourceAggregate.toLowerCase();
            }
            // PRIORITY 2: Search all aggregates for event publisher (works for CRUD events)
            else if (options?.allAggregates && options.allAggregates.length > 0) {
                const found = this.findEventPublisher(eventTypeName, options.allAggregates);
                if (found) {
                    sourceAggregateName = found;
                } else {
                    console.warn(`Warning: Could not find publisher aggregate for event ${eventTypeName}`);
                    // Fallback to simple name matching
                    const entityName = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
                    sourceAggregateName = entityName.toLowerCase();
                }
            }
            // PRIORITY 3: Fallback (only when allAggregates not available)
            else {
                console.warn(`Warning: allAggregates not available for event ${eventTypeName}`);
                const entityName = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
                sourceAggregateName = entityName.toLowerCase();
            }

            // Extract entity name from event (e.g., UpdateTopicEvent -> Topic)
            const entityName = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
            const subscriptionClassName = `${aggregateName}Subscribes${entityName}`;
            
            // Find projection entity
            const entities = getEntities(aggregate);
            const projectionEntities = entities.filter((e: any) => {
                const aggregateRef = e.aggregateRef;
                return aggregateRef && aggregateRef.toLowerCase() === entityName.toLowerCase();
            });
            
            const projectionEntity = projectionEntities.length > 0 ? projectionEntities[0] : null;
            const projectionEntityName = projectionEntity ? projectionEntity.name : null;
            const projectionPart = projectionEntityName ? projectionEntityName.replace(new RegExp(`^${aggregateName}`, 'i'), '') : entityName;
            
            // Build aggregateId and version field names
            const aggregateIdField = `${projectionPart.charAt(0).toLowerCase() + projectionPart.slice(1)}AggregateId`;
            const versionField = `${projectionPart.charAt(0).toLowerCase() + projectionPart.slice(1)}Version`;
            const capitalizedAggregateIdField = aggregateIdField.charAt(0).toUpperCase() + aggregateIdField.slice(1);
            const capitalizedVersionField = versionField.charAt(0).toUpperCase() + versionField.slice(1);
            
            return {
                eventType: eventTypeName.replace(/Event$/, ''),
                eventTypeName: eventTypeName,
                capitalizedEventName: eventTypeName,
                fullEventName: eventTypeName,
                capitalizedSubscriptionName: subscriptionClassName,
                subscriptionName: subscriptionClassName,
                sourceAggregate: sourceAggregateName,
                targetAggregate: aggregateName.toLowerCase(),
                projectionEntityName: projectionEntityName,
                projectionPart: projectionPart.charAt(0).toLowerCase() + projectionPart.slice(1),
                aggregateIdField: aggregateIdField,
                versionField: versionField,
                capitalizedAggregateIdField: capitalizedAggregateIdField,
                capitalizedVersionField: capitalizedVersionField,
                eventTypePackage: `${basePackage}.${projectName}.microservices.${sourceAggregateName}.events.publish`,
                isAsync: true,
                transactionPhase: 'AFTER_COMMIT'
            };
        });
    }

    private buildEventSubscriptionsImports(aggregate: Aggregate, options: EventGenerationOptions, eventSubscriptions: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const projectName = options?.projectName?.toLowerCase() || 'unknown';
        const basePackage = (this as any).getBasePackage?.() || 'pt.ulisboa.tecnico.socialsoftware';

        const imports = [
            ...baseImports,
            `import ${basePackage}.ms.domain.event.Event;`,
            `import ${basePackage}.ms.domain.event.EventSubscription;`
        ];

        // Add imports for projection entities and events
        eventSubscriptions.forEach((sub: any) => {
            if (sub.projectionEntityName) {
                imports.push(`import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${sub.projectionEntityName};`);
            }
            imports.push(`import ${sub.eventTypePackage}.${sub.eventTypeName};`);
        });

        imports.push('');
        return imports;
    }

    private async generateIndividualEventSubscription(context: any): Promise<string> {
        const template = this.loadTemplate('events/event-subscription.hbs');
        return this.renderTemplate(template, context);
    }
}
