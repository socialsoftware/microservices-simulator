import { AggregateExt } from "../../../../types/ast-extensions.js";

/**
 * Event Publisher Discovery
 *
 * Responsible for discovering which aggregate publishes a given event.
 * Searches through custom events, CRUD events, and projection entity events.
 */
export class EventPublisherDiscovery {
    /**
     * Find the aggregate that publishes a specific event.
     *
     * Search strategy (in priority order):
     * 1. Custom published events (explicitly defined in Events block)
     * 2. Root entity CRUD events (auto-generated for @GenerateCrud)
     * 3. Projection entity CRUD events (UpdatedEvent, DeletedEvent, RemovedEvent)
     *
     * @param eventTypeName Name of the event (e.g., "UserDeletedEvent")
     * @param allAggregates All aggregates in the model
     * @returns Lowercase aggregate name that publishes the event, or null if not found
     */
    static findEventPublisher(eventTypeName: string, allAggregates: AggregateExt[]): string | null {
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
