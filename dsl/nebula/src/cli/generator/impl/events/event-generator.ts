import { Aggregate } from "../../../../language/generated/ast.js";
import { EventGenerationOptions } from "./event-types.js";
import { EventHandlingGenerator } from "./event-handling-generator.js";
import { EventHandlerGenerator } from "./event-handler-generator.js";
import { PublishedEventGenerator } from "./published-event-generator.js";
import { EventSubscriptionGenerator } from "./event-subscription-generator.js";

export { EventGenerationOptions } from "./event-types.js";

export class EventGenerator {
    private eventHandlingGenerator = new EventHandlingGenerator();
    private eventHandlerGenerator = new EventHandlerGenerator();
    private publishedEventGenerator = new PublishedEventGenerator();
    private eventSubscriptionGenerator = new EventSubscriptionGenerator();

    async generateEvents(aggregate: Aggregate, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string } = {};

        results['event-handling'] = await this.eventHandlingGenerator.generateEventHandling(aggregate, rootEntity, options);

        const eventHandlers = await this.eventHandlerGenerator.generateEventHandlers(aggregate, rootEntity, options);
        Object.assign(results, eventHandlers);

        const publishedEvents = await this.publishedEventGenerator.generatePublishedEvents(aggregate, rootEntity, options);
        Object.assign(results, publishedEvents);

        const eventSubscriptions = await this.eventSubscriptionGenerator.generateEventSubscriptions(aggregate, rootEntity, options);
        Object.assign(results, eventSubscriptions);

        return results;
    }
}
