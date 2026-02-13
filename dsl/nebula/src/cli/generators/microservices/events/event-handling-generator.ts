import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventContext } from "./event-types.js";
import { EventNameParser } from "../../common/utils/event-name-parser.js";

export class EventHandlingGenerator extends EventBaseGenerator {
    async generateEventHandling(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<string> {
        const context = this.buildEventHandlingContext(aggregate, rootEntity, options);
        const template = this.getEventHandlingTemplate();
        return this.renderTemplate(template, context);
    }

    private buildEventHandlingContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventSubscriptions = this.buildEventSubscriptions(aggregate, rootEntity, baseContext.aggregateName, options);
        const imports = this.buildEventHandlingImports(aggregate, options, eventSubscriptions);
        const projectName = options?.projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getBasePackage(options);

        return {
            ...baseContext,
            packageName: `${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.events.handling`,
            coordinationPackage: `${basePackage}.${projectName}.coordination.eventProcessing`,
            aggregatePackage: `${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.aggregate`,
            lowerAggregateName: aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1),
            imports: imports.join('\n'),
            subscribedEvents: eventSubscriptions,
            subscribedEventImports: eventSubscriptions.map((sub: any) => ({
                handlerPackage: sub.handlerPackage,
                handlerName: sub.handlerName,
                eventPackage: sub.eventPackage,
                eventName: sub.eventName
            }))
        } as any;
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
        const basePackage = this.getBasePackage(options);

        return simpleSubscriptions.map((sub: any) => {
            const eventTypeName = sub.eventType?.ref?.name || sub.eventType?.$refText || 'UnknownEvent';

            // Extract entity name from event (e.g., UserDeletedEvent -> User, TopicUpdatedEvent -> Topic)
            const entityName = EventNameParser.extractEntityName(eventTypeName);

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
                    sourceAggregateName = entityName.toLowerCase();
                }
            }
            // PRIORITY 3: Fallback (only when allAggregates not available)
            else {
                // Fallback: infer source aggregate from event name
                // UserDeletedEvent -> user, TopicUpdatedEvent -> topic
                sourceAggregateName = entityName.toLowerCase();
            }

            const handlerName = `${entityName}EventHandler`;

            return {
                eventName: eventTypeName,
                handlerName: handlerName,
                eventPackage: `${basePackage}.${projectName}.microservices.${sourceAggregateName}.events.publish`,
                handlerPackage: `${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.events.handling.handlers`,
                sourceAggregate: sourceAggregateName,
                isExternal: sourceAggregateName !== aggregateName.toLowerCase()
            };
        });
    }

    private buildEventHandlingImports(aggregate: Aggregate, options: EventGenerationOptions, eventSubscriptions: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const projectName = options?.projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getBasePackage(options);

        const imports = [
            ...baseImports,
            `import ${basePackage}.ms.domain.event.EventApplicationService;`,
            `import ${basePackage}.${projectName}.coordination.eventProcessing.${this.capitalize(aggregate.name)}EventProcessing;`,
            `import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${this.capitalize(aggregate.name)}Repository;`
        ];

        // Add imports for each subscribed event and its handler
        eventSubscriptions.forEach((sub: any) => {
            imports.push(`import ${sub.eventPackage}.${sub.eventName};`);
            imports.push(`import ${sub.handlerPackage}.${sub.handlerName};`);
        });

        return imports;
    }

    private getEventHandlingTemplate(): string {
        return this.loadTemplate('events/event-handling.hbs');
    }
}
