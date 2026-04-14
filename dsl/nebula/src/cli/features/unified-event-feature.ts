import { Aggregate } from "../../language/generated/ast.js";
import { EventGenerator } from "../generators/microservices/events/event-orchestrator.js";
import { PublishedEventGenerator } from "../generators/microservices/events/event-class-generator.js";
import { ReferencesGenerator } from "../generators/microservices/events/references-generator.js";
import { GenerationOptions, GeneratorRegistry } from "../engine/types.js";
import { FileWriter } from "../utils/file-writer.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../utils/error-handler.js";
import * as path from 'path';

interface EventGenerationConfig {
    generateCustomEvents: boolean;
    generateStandardEvents: boolean;
    generateEventHandlers: boolean;
    generateEventSubscriptions: boolean;
}

export class UnifiedEventFeature {
    private eventGenerator: EventGenerator;
    private publishedEventGenerator: PublishedEventGenerator;
    private referencesGenerator: ReferencesGenerator;

    constructor() {
        this.eventGenerator = new EventGenerator();
        this.publishedEventGenerator = new PublishedEventGenerator();
        this.referencesGenerator = new ReferencesGenerator();
    }

    async generateEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry,
        config: EventGenerationConfig = this.getDefaultConfig()
    ): Promise<void> {
        if (!aggregate.events && !config.generateStandardEvents) {
            return;
        }

        if (config.generateStandardEvents) {
            await this.generateStandardEvents(aggregate, aggregatePath, options, generators);
        }

        if (config.generateCustomEvents && aggregate.events) {
            await this.generateCustomEvents(aggregate, aggregatePath, options);
        }
    }

    private getSharedEventsPath(aggregatePath: string): string {
        const parts = aggregatePath.split('/');
        const msIndex = parts.lastIndexOf('microservices');
        if (msIndex !== -1) {
            return parts.slice(0, msIndex).join('/') + '/events';
        }
        return path.join(aggregatePath, '..', '..', 'events');
    }

    private async generateStandardEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        await ErrorHandler.wrapAsync(
            async () => {

                const allAggregates = options.allModels?.flatMap((model: any) => model.aggregates) || [];

                const eventCode = await generators.eventGenerator.generateEvents(aggregate, {
                    ...options,
                    allAggregates
                });


                const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
                const hasGenerateCrud = (aggregate as any).generateCrud;

                if (hasGenerateCrud && rootEntity) {
                    const crudEvents = await this.publishedEventGenerator.generatePublishedEvents(
                        aggregate,
                        rootEntity,
                        {
                            projectName: options.projectName || 'unknown',
                            basePackage: options.basePackage,
                            allAggregates
                        }
                    );

                    Object.assign(eventCode, crudEvents);
                }

                if (eventCode['event-handling']) {
                    const eventHandlingPath = path.join(aggregatePath, 'events', 'handling', `${aggregate.name}EventHandling.java`);
                    await FileWriter.writeGeneratedFile(
                        eventHandlingPath,
                        eventCode['event-handling'],
                        `event handling ${aggregate.name}EventHandling`
                    );
                }

                await this.generateEventHandlersFromCode(eventCode, aggregatePath);

                await this.generatePublishedEventsFromCode(eventCode, aggregatePath);

                await this.generateEventSubscriptionsFromCode(eventCode, aggregatePath);

                if (generators.eventHandlerGenerator && rootEntity) {
                    const individualEventHandlers = await generators.eventHandlerGenerator.generateEventHandlers(aggregate, rootEntity, {
                        ...options,
                        allAggregates
                    });
                    await this.generateIndividualEventHandlers(individualEventHandlers, aggregatePath);
                }


                if ((aggregate as any).references && rootEntity) {
                    await this.generateReferenceHandlers(aggregate, rootEntity, aggregatePath, options, allAggregates);
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

    private async generateCustomEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions
    ): Promise<void> {
        if (aggregate.events!.publishedEvents) {
            await this.generateCustomPublishedEvents(aggregate, aggregatePath, options);
        }

        if (aggregate.events!.subscribedEvents && aggregate.events!.subscribedEvents.length > 0) {
            await this.generateCustomBaseEventHandler(aggregate, aggregatePath, options);
        }

        if (aggregate.events!.subscribedEvents) {
            await this.generateCustomSubscribedEvents(aggregate, aggregatePath, options);
        }
    }

    private async generateCustomPublishedEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions
    ): Promise<void> {
        const sharedEventsPath = this.getSharedEventsPath(aggregatePath);
        for (const publishedEvent of aggregate.events!.publishedEvents!) {
            await ErrorHandler.wrapAsync(
                async () => {
                    const eventCode = this.eventGenerator.generatePublishedEvent(publishedEvent, aggregate, options);
                    const eventPath = path.join(sharedEventsPath, `${publishedEvent.name}.java`);
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

    private async generateCustomSubscribedEvents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions
    ): Promise<void> {

        const allAggregates = options.allModels?.flatMap((model: any) => model.aggregates) || [];
        const enhancedOptions = { ...options, allAggregates };

        const directSubscribed = aggregate.events?.subscribedEvents || [];
        const interSubscribed = (aggregate.events as any)?.interInvariants?.flatMap((ii: any) =>
            (ii?.subscribedEvents || []).map((sub: any) => ({ ...sub, isInterInvariant: true, interInvariantName: ii.name }))
        ) || [];
        const allSubscribed = [...directSubscribed, ...interSubscribed];

        const eventMap = new Map<string, any>();
        allSubscribed.forEach((event: any) => {
            const eventTypeName = event.eventType || 'UnknownEvent';


            const mapKey = event.isInterInvariant
                ? `${eventTypeName}:${event.interInvariantName}`
                : eventTypeName;
            if (!eventMap.has(mapKey)) {
                eventMap.set(mapKey, event);
            }
        });
        const uniqueSubscribed = Array.from(eventMap.values());

        for (const subscribedEvent of uniqueSubscribed) {
            await ErrorHandler.wrapAsync(
                async () => {
                    const subscriptionCode = this.eventGenerator.generateSubscribedEvent(subscribedEvent, aggregate, enhancedOptions);
                    const eventTypeName = (subscribedEvent as any).eventType || 'UnknownEvent';


                    let subscriptionName = `${aggregate.name}Subscribes${eventTypeName.replace('Event', '')}`;
                    if ((subscribedEvent as any).isInterInvariant && (subscribedEvent as any).interInvariantName) {

                        const interInvariantName = (subscribedEvent as any).interInvariantName;
                        const interInvariantSuffix = interInvariantName
                            .split('_')
                            .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
                            .join('');
                        subscriptionName = `${aggregate.name}Subscribes${eventTypeName.replace('Event', '')}${interInvariantSuffix}`;
                    }

                    const subscriptionPath = path.join(aggregatePath, 'events', 'subscribe', `${subscriptionName}.java`);
                    await FileWriter.writeGeneratedFile(subscriptionPath, subscriptionCode, `subscribed event ${subscriptionName}`);



                    const isInterInvariant = (subscribedEvent as any).isInterInvariant;
                    if (!isInterInvariant) {
                        const handlerCode = this.eventGenerator.generateEventHandler(subscribedEvent, aggregate, enhancedOptions);
                        const handlerName = `${eventTypeName}Handler`;
                        const handlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${handlerName}.java`);
                        await FileWriter.writeGeneratedFile(handlerPath, handlerCode, `event handler ${handlerName}`);
                    }
                },
                ErrorUtils.entityContext(
                    'generate custom subscribed event',
                    aggregate.name,
                    (subscribedEvent as any).eventType || 'UnknownEvent',
                    'event-generator'
                ),
                ErrorSeverity.ERROR
            );
        }
    }

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
        const sharedEventsPath = this.getSharedEventsPath(aggregatePath);

        const writePromises = publishedEventEntries.map(([key, content]) => {
            const eventName = key.replace('published-event-', '');
            const className = this.extractClassNameFromContent(content as string);
            const fileName = className || eventName;
            const eventPath = path.join(sharedEventsPath, `${fileName}.java`);
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

    private async generateReferenceHandlers(
        aggregate: Aggregate,
        rootEntity: any,
        aggregatePath: string,
        options: GenerationOptions,
        allAggregates: any[]
    ): Promise<void> {
        const baseHandlerPath = path.join(aggregatePath, 'events', 'handling', 'handlers', `${aggregate.name}EventHandler.java`);
        const fs = await import('fs');
        if (!fs.existsSync(baseHandlerPath)) {
            const baseHandlerCode = this.eventGenerator.generateBaseEventHandler(aggregate, options);
            await FileWriter.writeGeneratedFile(baseHandlerPath, baseHandlerCode, `base event handler ${aggregate.name}EventHandler`);
        }

        const referenceHandlers = await this.referencesGenerator.generateReferenceHandlers(
            aggregate,
            rootEntity,
            {
                projectName: options.projectName || 'unknown',
                basePackage: options.basePackage,
                allAggregates
            }
        );


        for (const [key, code] of Object.entries(referenceHandlers)) {
            if (key.startsWith('ref-subscription-')) {
                const targetAggregate = key.replace('ref-subscription-', '');
                const className = `${aggregate.name}Subscribes${targetAggregate}Deleted`;
                const subscriptionPath = path.join(
                    aggregatePath,
                    'events',
                    'subscribe',
                    `${className}.java`
                );
                await FileWriter.writeGeneratedFile(subscriptionPath, code, `reference subscription ${className}`);
            } else if (key.startsWith('ref-handler-')) {
                const targetAggregate = key.replace('ref-handler-', '');
                const className = `${targetAggregate}DeletedEventHandler`;
                const handlerPath = path.join(
                    aggregatePath,
                    'events',
                    'handling',
                    'handlers',
                    `${className}.java`
                );
                await FileWriter.writeGeneratedFile(handlerPath, code, `reference handler ${className}`);
            }
        }
    }

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
            'src', 'main', 'java', ...options.basePackage.split('.'),
            options.projectName.toLowerCase(), 'microservices', aggregate.name.toLowerCase()
        );

        await feature.generateCustomEvents(aggregate, aggregatePath, options);
    }
}
