import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions, Aggregate, GeneratorRegistry } from "../core/types.js";
import { EventFeature } from "./event-feature.js";

export class EventsFeature {
    static async generateEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        try {
            const eventCode = await generators.eventGenerator.generateEvents(aggregate, options);

            const eventHandlingPath = path.join(aggregatePath, 'events', 'handling', `${aggregate.name}EventHandling.java`);
            await fs.mkdir(path.dirname(eventHandlingPath), { recursive: true });
            await fs.writeFile(eventHandlingPath, eventCode['event-handling'], 'utf-8');
            console.log(`\t- Generated event handling ${aggregate.name}EventHandling`);

            for (const [key, content] of Object.entries(eventCode)) {
                if (key.startsWith('event-handler-')) {
                    const handlerName = key.replace('event-handler-', '');
                    const handlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${handlerName}.java`);
                    await fs.mkdir(path.dirname(handlerPath), { recursive: true });
                    await fs.writeFile(handlerPath, content as string, 'utf-8');
                    console.log(`\t- Generated event handler ${handlerName}`);
                }
            }

            for (const [key, content] of Object.entries(eventCode)) {
                if (key.startsWith('published-event-')) {
                    const eventName = key.replace('published-event-', '');

                    const className = this.extractClassNameFromContent(content as string);
                    const fileName = className || eventName;

                    const eventPath = path.join(aggregatePath, 'events', 'publish', `${fileName}.java`);
                    await fs.mkdir(path.dirname(eventPath), { recursive: true });
                    await fs.writeFile(eventPath, content as string, 'utf-8');
                    console.log(`\t- Generated published event ${fileName}`);
                }
            }

            for (const [key, content] of Object.entries(eventCode)) {
                if (key.startsWith('event-subscription-')) {
                    const subscriptionName = key.replace('event-subscription-', '');
                    const subscriptionPath = path.join(aggregatePath, 'events', 'subscribe', `Subscribes${subscriptionName}.java`);
                    await fs.mkdir(path.dirname(subscriptionPath), { recursive: true });
                    await fs.writeFile(subscriptionPath, content as string, 'utf-8');
                    console.log(`\t- Generated event subscription Subscribes${subscriptionName}`);
                }
            }

            const individualEventHandlers = await generators.eventHandlerGenerator.generateEventHandlers(aggregate, options);
            for (const [key, content] of Object.entries(individualEventHandlers)) {
                if (key.startsWith('specific-handler-')) {
                    const handlerName = key.replace('specific-handler-', '');
                    const handlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${handlerName}.java`);
                    await fs.mkdir(path.dirname(handlerPath), { recursive: true });
                    await fs.writeFile(handlerPath, content as string, 'utf-8');
                    console.log(`\t- Generated specific event handler ${handlerName}`);
                }
            }
        } catch (error) {
            console.error(`\t- Error generating events for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }

        // Generate custom events defined in DSL
        const customEventFeature = new EventFeature();
        await customEventFeature.generateEvents(aggregate, options);
    }

    private static extractClassNameFromContent(content: string): string | null {
        const classMatch = content.match(/public\s+class\s+(\w+)/);
        return classMatch ? classMatch[1] : null;
    }
}
