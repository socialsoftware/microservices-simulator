import { AggregateExt } from "../../../../types/ast-extensions.js";



export class EventPublisherDiscovery {
    

    static findEventPublisher(eventTypeName: string, allAggregates: AggregateExt[]): string | null {
        for (const agg of allAggregates) {
            const aggName = agg.name;

            
            const aggregateEvents = (agg as any).events;
            const customEvents = aggregateEvents?.publishedEvents || [];
            if (customEvents.some((e: any) => e.name === eventTypeName)) {
                return aggName.toLowerCase();
            }

            
            if ((agg as any).generateCrud) {
                const rootCrudEvents = [
                    `${aggName}UpdatedEvent`,
                    `${aggName}DeletedEvent`
                ];
                if (rootCrudEvents.includes(eventTypeName)) {
                    return aggName.toLowerCase();
                }
            }

            
            const projectionEntities = (agg.entities || []).filter((e: any) =>
                !e.isRoot && e.aggregateRef
            );
            for (const proj of projectionEntities) {
                const projName = proj.name;
                const projCrudEvents = [
                    `${projName}UpdatedEvent`,
                    `${projName}DeletedEvent`,
                    `${projName}RemovedEvent`  
                ];
                if (projCrudEvents.includes(eventTypeName)) {
                    return aggName.toLowerCase();  
                }
            }
        }

        return null;  
    }
}
