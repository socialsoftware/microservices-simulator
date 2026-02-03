import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../common/orchestration-base.js";
import { EventGenerationOptions, EventContext } from "./event-types.js";

export abstract class EventBaseGenerator extends OrchestrationBase {
    protected createBaseEventContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventContext {
        const naming = this.createAggregateNaming(aggregate.name);
        const projectName = options?.projectName || 'unknown';
        const packageName = this.generatePackageName(projectName, aggregate.name, 'events', 'publish');

        return {
            aggregateName: naming.original,
            capitalizedAggregate: naming.capitalized,
            lowerAggregate: naming.lower,
            packageName,
            rootEntity,
            projectName,
            imports: this.buildStandardImports(projectName, aggregate.name)
        };
    }

    protected buildEventProperties(rootEntity: Entity, eventType: string): any[] {
        return this.buildPropertyInfo(rootEntity);
    }

    protected buildBaseImports(aggregate: Aggregate, options: EventGenerationOptions): string[] {
        const projectName = options?.projectName || 'unknown';
        const baseImports = this.buildStandardImports(projectName, aggregate.name);
        const eventImports = [
            'import org.springframework.context.ApplicationEvent;',
            'import org.springframework.context.event.EventListener;',
            'import org.springframework.scheduling.annotation.Async;',
            'import org.springframework.transaction.event.TransactionalEventListener;',
            'import org.springframework.transaction.event.TransactionPhase;',
        ];

        return this.combineImports(baseImports, eventImports);
    }

    protected getEventNameVariations(eventName: string, aggregateName: string) {
        const capitalizedAggregate = this.capitalize(aggregateName);

        return {
            eventName,
            capitalizedEventName: this.capitalize(eventName),
            lowerEventName: eventName.toLowerCase(),
            fullEventName: `${capitalizedAggregate}${this.capitalize(eventName)}Event`,
            handlerName: `${eventName}Handler`,
            capitalizedHandlerName: `${this.capitalize(eventName)}Handler`,
            subscriptionName: `${eventName}Subscription`,
            capitalizedSubscriptionName: `${this.capitalize(eventName)}Subscription`
        };
    }

    /**
     * Find which aggregate publishes a given event by checking all aggregates' published events.
     * Handles both custom published events and auto-generated CRUD events.
     * This is a shared helper used by multiple event generators.
     */
    protected findEventPublisher(eventTypeName: string, allAggregates: Aggregate[]): string | null {
        for (const agg of allAggregates) {
            const aggName = agg.name;

            // 1. Check custom published events (explicitly defined in DSL)
            const aggregateEvents = (agg as any).events;
            const customEvents = aggregateEvents?.publishedEvents || [];
            if (customEvents.some((e: any) => e.name === eventTypeName)) {
                return aggName.toLowerCase();
            }

            // 2. Check root entity CRUD events (auto-generated for @GenerateCrud)
            if (agg.generateCrud) {
                const rootCrudEvents = [
                    `${aggName}UpdatedEvent`,
                    `${aggName}DeletedEvent`
                ];
                if (rootCrudEvents.includes(eventTypeName)) {
                    return aggName.toLowerCase();
                }
            }

            // 3. Check projection entity CRUD events
            const projectionEntities = (agg.entities || []).filter((e: any) =>
                !e.isRoot && e.aggregateRef
            );
            for (const proj of projectionEntities) {
                const projName = proj.name;
                const projCrudEvents = [
                    `${projName}UpdatedEvent`,
                    `${projName}DeletedEvent`,
                    `${projName}RemovedEvent`  // For collection manipulation
                ];
                if (projCrudEvents.includes(eventTypeName)) {
                    return aggName.toLowerCase();  // Return AGGREGATE name, not projection name
                }
            }
        }

        return null;  // Not found in any aggregate
    }
}
