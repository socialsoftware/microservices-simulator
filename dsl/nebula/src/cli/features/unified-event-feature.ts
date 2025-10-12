/**
 * Unified Event Feature System
 * 
 * This module consolidates event-feature.ts and events-feature.ts into a single,
 * comprehensive event generation system that handles both DSL-defined custom events
 * and generator-produced standard events with consistent patterns.
 */

import { Aggregate } from "../../language/generated/ast.js";
import { EventGenerator } from "../generators/microservices/events/event-generator.js";
import { GenerationOptions, GeneratorRegistry } from "../engine/types.js";
import { FileWriter } from "../utils/file-writer.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../utils/error-handler.js";
import * as path from 'path';

/**
 * Event generation configuration
 */
interface EventGenerationConfig {
    generateCustomEvents: boolean;
    generateStandardEvents: boolean;
    generateEventHandlers: boolean;
    generateEventSubscriptions: boolean;
}

/**
 * Unified event feature that handles all types of event generation
 */
export class UnifiedEventFeature {
    private eventGenerator: EventGenerator;

    constructor() {
        this.eventGenerator = new EventGenerator();
    }

    /**
     * Main entry point for event generation - handles both custom and standard events
     */
    async generateEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry,
        config: EventGenerationConfig = this.getDefaultConfig()
    ): Promise<void> {
        if (!aggregate.events && !config.generateStandardEvents) {
            console.log(`\t- No events defined for ${aggregate.name}`);
            return;
        }

        // Generate standard events (from generators)
        if (config.generateStandardEvents) {
            await this.generateStandardEvents(aggregate, aggregatePath, options, generators);
        }

        // Generate custom DSL-defined events
        if (config.generateCustomEvents && aggregate.events) {
            await this.generateCustomEvents(aggregate, aggregatePath, options);
        }
    }

    /**
     * Generate standard events produced by generators
     */
    private async generateStandardEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        await ErrorHandler.wrapAsync(
            async () => {
                const eventCode = await generators.eventGenerator.generateEvents(aggregate, options);

                // Generate main event handling class
                if (eventCode['event-handling']) {
                    const eventHandlingPath = path.join(aggregatePath, 'events', 'handling', `${aggregate.name}EventHandling.java`);
                    await FileWriter.writeGeneratedFile(
                        eventHandlingPath,
                        eventCode['event-handling'],
                        `event handling ${aggregate.name}EventHandling`
                    );
                }

                // Generate event handlers
                await this.generateEventHandlersFromCode(eventCode, aggregatePath);

                // Generate published events
                await this.generatePublishedEventsFromCode(eventCode, aggregatePath);

                // Generate event subscriptions
                await this.generateEventSubscriptionsFromCode(eventCode, aggregatePath);

                // Generate individual event handlers
                if (generators.eventHandlerGenerator) {
                    const individualEventHandlers = await generators.eventHandlerGenerator.generateEventHandlers(aggregate, options);
                    await this.generateIndividualEventHandlers(individualEventHandlers, aggregatePath);
                }
            },
            ErrorUtils.aggregateContext(
                'generate standard events',
                aggregate.name,
                'unified-event-feature',
                { hasEvents: !!aggregate.events }
            ),
            ErrorSeverity.ERROR
        );
    }

    /**
     * Generate custom DSL-defined events
     */
    private async generateCustomEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions
    ): Promise<void> {
        // Generate published events
        if (aggregate.events!.publishedEvents) {
            await this.generateCustomPublishedEvents(aggregate, aggregatePath, options);
        }

        // Generate base event handler for subscribed events
        if (aggregate.events!.subscribedEvents && aggregate.events!.subscribedEvents.length > 0) {
            await this.generateCustomBaseEventHandler(aggregate, aggregatePath, options);
        }

        // Generate subscribed events and their handlers
        if (aggregate.events!.subscribedEvents) {
            await this.generateCustomSubscribedEvents(aggregate, aggregatePath, options);
        }
    }

    /**
     * Generate custom published events from DSL
     */
    private async generateCustomPublishedEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions
    ): Promise<void> {
        for (const publishedEvent of aggregate.events!.publishedEvents!) {
            await ErrorHandler.wrapAsync(
                async () => {
                    const eventCode = this.eventGenerator.generatePublishedEvent(publishedEvent, aggregate, options);
                    const eventPath = path.join(aggregatePath, 'events', 'publish', `${publishedEvent.name}.java`);
                    await FileWriter.writeGeneratedFile(eventPath, eventCode, `published event ${publishedEvent.name}`);
                },
                ErrorUtils.entityContext(
                    'generate custom published event',
                    aggregate.name,
                    publishedEvent.name,
                    'event-generator'
                ),
                ErrorSeverity.ERROR
            );
        }
    }

    /**
     * Generate custom base event handler
     */
    private async generateCustomBaseEventHandler(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions
    ): Promise<void> {
        await ErrorHandler.wrapAsync(
            async () => {
                const baseHandlerCode = this.eventGenerator.generateBaseEventHandler(aggregate, options);
                const baseHandlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${aggregate.name}EventHandler.java`);
                await FileWriter.writeGeneratedFile(baseHandlerPath, baseHandlerCode, `base event handler ${aggregate.name}EventHandler`);
            },
            ErrorUtils.aggregateContext(
                'generate custom base event handler',
                aggregate.name,
                'event-generator'
            ),
            ErrorSeverity.ERROR
        );
    }

    /**
     * Generate custom subscribed events and their handlers
     */
    private async generateCustomSubscribedEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions
    ): Promise<void> {
        for (const subscribedEvent of aggregate.events!.subscribedEvents!) {
            await ErrorHandler.wrapAsync(
                async () => {
                    // Generate subscription class
                    const subscriptionCode = this.eventGenerator.generateSubscribedEvent(subscribedEvent, aggregate, options);
                    const eventTypeName = subscribedEvent.eventType.ref?.name || subscribedEvent.eventType.$refText || 'UnknownEvent';
                    const subscriptionName = `${aggregate.name}Subscribes${eventTypeName.replace('Event', '')}`;
                    const subscriptionPath = path.join(aggregatePath, 'events', 'subscribe', `${subscriptionName}.java`);
                    await FileWriter.writeGeneratedFile(subscriptionPath, subscriptionCode, `subscribed event ${subscriptionName}`);

                    // Generate event handler
                    const handlerCode = this.eventGenerator.generateEventHandler(subscribedEvent, aggregate, options);
                    const handlerName = `${eventTypeName}Handler`;
                    const handlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${handlerName}.java`);
                    await FileWriter.writeGeneratedFile(handlerPath, handlerCode, `event handler ${handlerName}`);
                },
                ErrorUtils.entityContext(
                    'generate custom subscribed event',
                    aggregate.name,
                    subscribedEvent.eventType.ref?.name || subscribedEvent.eventType.$refText || 'UnknownEvent',
                    'event-generator'
                ),
                ErrorSeverity.ERROR
            );
        }
    }

    /**
     * Helper methods for processing generator-produced events
     */
    private async generateEventHandlersFromCode(eventCode: any, aggregatePath: string): Promise<void> {
        const handlerEntries = Object.entries(eventCode).filter(([key]) => key.startsWith('event-handler-'));

        const writePromises = handlerEntries.map(([key, content]) => {
            const handlerName = key.replace('event-handler-', '');
            const handlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${handlerName}.java`);
            return FileWriter.writeGeneratedFile(handlerPath, content as string, `event handler ${handlerName}`);
        });

        await Promise.all(writePromises);
    }

    private async generatePublishedEventsFromCode(eventCode: any, aggregatePath: string): Promise<void> {
        const publishedEventEntries = Object.entries(eventCode).filter(([key]) => key.startsWith('published-event-'));

        const writePromises = publishedEventEntries.map(([key, content]) => {
            const eventName = key.replace('published-event-', '');
            const className = this.extractClassNameFromContent(content as string);
            const fileName = className || eventName;
            const eventPath = path.join(aggregatePath, 'events', 'publish', `${fileName}.java`);
            return FileWriter.writeGeneratedFile(eventPath, content as string, `published event ${fileName}`);
        });

        await Promise.all(writePromises);
    }

    private async generateEventSubscriptionsFromCode(eventCode: any, aggregatePath: string): Promise<void> {
        const subscriptionEntries = Object.entries(eventCode).filter(([key]) => key.startsWith('event-subscription-'));

        const writePromises = subscriptionEntries.map(([key, content]) => {
            const subscriptionName = key.replace('event-subscription-', '');
            const subscriptionPath = path.join(aggregatePath, 'events', 'subscribe', `Subscribes${subscriptionName}.java`);
            return FileWriter.writeGeneratedFile(subscriptionPath, content as string, `event subscription Subscribes${subscriptionName}`);
        });

        await Promise.all(writePromises);
    }

    private async generateIndividualEventHandlers(individualEventHandlers: any, aggregatePath: string): Promise<void> {
        const handlerEntries = Object.entries(individualEventHandlers).filter(([key]) => key.startsWith('specific-handler-'));

        const writePromises = handlerEntries.map(([key, content]) => {
            const handlerName = key.replace('specific-handler-', '');
            const handlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${handlerName}.java`);
            return FileWriter.writeGeneratedFile(handlerPath, content as string, `specific event handler ${handlerName}`);
        });

        await Promise.all(writePromises);
    }

    /**
     * Utility methods
     */
    private extractClassNameFromContent(content: string): string | null {
        const classMatch = content.match(/public\s+class\s+(\w+)/);
        return classMatch ? classMatch[1] : null;
    }

    private getDefaultConfig(): EventGenerationConfig {
        return {
            generateCustomEvents: true,
            generateStandardEvents: true,
            generateEventHandlers: true,
            generateEventSubscriptions: true
        };
    }

    /**
     * Static convenience methods for backward compatibility
     */
    static async generateEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        const feature = new UnifiedEventFeature();
        await feature.generateEvents(aggregate, aggregatePath, options, generators);
    }

    static async generateCustomEventsOnly(
        aggregate: Aggregate,
        options: GenerationOptions
    ): Promise<void> {
        const feature = new UnifiedEventFeature();
        const aggregatePath = path.join(
            options.outputPath!,
            'src', 'main', 'java', 'pt', 'ulisboa', 'tecnico', 'socialsoftware',
            options.projectName.toLowerCase(), 'microservices', aggregate.name.toLowerCase()
        );

        await feature.generateCustomEvents(aggregate, aggregatePath, options);
    }
}
