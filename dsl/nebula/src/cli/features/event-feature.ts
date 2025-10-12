import { Aggregate } from "../../language/generated/ast.js";
import { EventGenerator } from "../generators/microservices/events/event-generator.js";
import { GenerationOptions } from "../engine/types.js";
import * as fs from 'fs/promises';
import * as path from 'path';

export class EventFeature {
    private eventGenerator: EventGenerator;

    constructor() {
        this.eventGenerator = new EventGenerator();
    }

    async generateEvents(aggregate: Aggregate, options: GenerationOptions): Promise<void> {
        if (!aggregate.events) {
            console.log(`\t- No events defined for ${aggregate.name}`);
            return;
        }

        const aggregatePath = path.join(options.outputPath!, 'src', 'main', 'java', 'pt', 'ulisboa', 'tecnico', 'socialsoftware', options.projectName.toLowerCase(), 'microservices', aggregate.name.toLowerCase());

        // Generate published events
        if (aggregate.events.publishedEvents) {
            for (const publishedEvent of aggregate.events.publishedEvents) {
                try {
                    const eventCode = this.eventGenerator.generatePublishedEvent(publishedEvent, aggregate, options);
                    const eventPath = path.join(aggregatePath, 'events', 'publish', `${publishedEvent.name}.java`);
                    await fs.mkdir(path.dirname(eventPath), { recursive: true });
                    await fs.writeFile(eventPath, eventCode, 'utf-8');
                    console.log(`\t- Generated published event ${publishedEvent.name}`);
                } catch (error) {
                    console.error(`Error generating published event ${publishedEvent.name}:`, error);
                    throw error;
                }
            }
        }

        // Generate base event handler (if there are any subscribed events)
        if (aggregate.events.subscribedEvents && aggregate.events.subscribedEvents.length > 0) {
            try {
                const baseHandlerCode = this.eventGenerator.generateBaseEventHandler(aggregate, options);
                const baseHandlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${aggregate.name}EventHandler.java`);
                await fs.mkdir(path.dirname(baseHandlerPath), { recursive: true });
                await fs.writeFile(baseHandlerPath, baseHandlerCode, 'utf-8');
                console.log(`\t- Generated base event handler ${aggregate.name}EventHandler`);
            } catch (error) {
                console.error(`Error generating base event handler:`, error);
                throw error;
            }
        }

        // Generate subscribed events
        if (aggregate.events.subscribedEvents) {
            for (const subscribedEvent of aggregate.events.subscribedEvents) {
                try {
                    // Generate subscription class
                    const subscriptionCode = this.eventGenerator.generateSubscribedEvent(subscribedEvent, aggregate, options);
                    const eventTypeName = subscribedEvent.eventType.ref?.name || subscribedEvent.eventType.$refText || 'UnknownEvent';
                    const subscriptionName = `${aggregate.name}Subscribes${eventTypeName.replace('Event', '')}`;
                    const subscriptionPath = path.join(aggregatePath, 'events', 'subscribe', `${subscriptionName}.java`);
                    await fs.mkdir(path.dirname(subscriptionPath), { recursive: true });
                    await fs.writeFile(subscriptionPath, subscriptionCode, 'utf-8');
                    console.log(`\t- Generated subscribed event ${subscriptionName}`);

                    // Generate event handler
                    const handlerCode = this.eventGenerator.generateEventHandler(subscribedEvent, aggregate, options);
                    const handlerName = `${eventTypeName}Handler`;
                    const handlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${handlerName}.java`);
                    await fs.mkdir(path.dirname(handlerPath), { recursive: true });
                    await fs.writeFile(handlerPath, handlerCode, 'utf-8');
                    console.log(`\t- Generated event handler ${handlerName}`);
                } catch (error) {
                    console.error(`Error generating subscribed event:`, error);
                    throw error;
                }
            }
        }
    }
}
