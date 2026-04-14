import { Aggregate } from "../../language/generated/ast.js";
import { EventGenerator } from "../generators/microservices/events/event-orchestrator.js";
import { EventClassGenerator } from "../generators/microservices/events/event-class-generator.js";
import { ReferencesGenerator } from "../generators/microservices/events/references-generator.js";
import { GenerationOptions, GeneratorRegistry } from "../engine/types.js";
import { FileWriter } from "../utils/file-writer.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../utils/error-handler.js";
import { AggregatePaths } from "../utils/path-builder.js";
import * as path from 'path';

function pathsFor(aggregatePath: string, aggregateName: string = ''): AggregatePaths {
    return new AggregatePaths(aggregatePath, aggregateName);
}

interface EventGenerationConfig {
    generateCustomEvents: boolean;
    generateStandardEvents: boolean;
    generateEventHandlers: boolean;
    generateEventSubscriptions: boolean;
}

export class UnifiedEventFeature {
    private eventGenerator: EventGenerator;
    private eventClassGenerator: EventClassGenerator;
    private referencesGenerator: ReferencesGenerator;

    constructor() {
        this.eventGenerator = new EventGenerator();
        this.eventClassGenerator = new EventClassGenerator();
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
                    const crudEvents = await this.eventClassGenerator.generatePublishedEvents(
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
                    const eventHandlingPath = pathsFor(aggregatePath, aggregate.name).eventHandling();
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
        const p = pathsFor(aggregatePath, aggregate.name);
        for (const publishedEvent of aggregate.events!.publishedEvents!) {
            await ErrorHandler.wrapAsync(
                async () => {
                    const eventCode = this.eventGenerator.generatePublishedEvent(publishedEvent, aggregate, options);
                    const eventPath = p.sharedEvent(publishedEvent.name);
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
                const baseHandlerPath = pathsFor(aggregatePath, aggregate.name).eventBaseHandler();
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

                    const p = pathsFor(aggregatePath, aggregate.name);
                    const subscriptionPath = p.eventSubscription(subscriptionName);
                    await FileWriter.writeGeneratedFile(subscriptionPath, subscriptionCode, `subscribed event ${subscriptionName}`);



                    const isInterInvariant = (subscribedEvent as any).isInterInvariant;
                    if (!isInterInvariant) {
                        const handlerCode = this.eventGenerator.generateEventHandler(subscribedEvent, aggregate, enhancedOptions);
                        const handlerName = `${eventTypeName}Handler`;
                        const handlerPath = p.eventHandler(handlerName);
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
        const p = pathsFor(aggregatePath);
        const handlerEntries = Object.entries(eventCode).filter(([key]) => key.startsWith('event-handler-'));

        const writePromises = handlerEntries.map(([key, content]) => {
            const handlerName = key.replace('event-handler-', '');
            return FileWriter.writeGeneratedFile(p.eventHandler(handlerName), content as string, `event handler ${handlerName}`);
        });

        await Promise.all(writePromises);
    }

    private async generatePublishedEventsFromCode(eventCode: any, aggregatePath: string): Promise<void> {
        const p = pathsFor(aggregatePath);
        const publishedEventEntries = Object.entries(eventCode).filter(([key]) => key.startsWith('published-event-'));

        const writePromises = publishedEventEntries.map(([key, content]) => {
            const eventName = key.replace('published-event-', '');
            const className = this.extractClassNameFromContent(content as string);
            const fileName = className || eventName;
            return FileWriter.writeGeneratedFile(p.sharedEvent(fileName), content as string, `published event ${fileName}`);
        });

        await Promise.all(writePromises);
    }

    private async generateEventSubscriptionsFromCode(eventCode: any, aggregatePath: string): Promise<void> {
        const p = pathsFor(aggregatePath);
        const subscriptionEntries = Object.entries(eventCode).filter(([key]) => key.startsWith('event-subscription-'));

        const writePromises = subscriptionEntries.map(([key, content]) => {
            const subscriptionName = key.replace('event-subscription-', '');
            return FileWriter.writeGeneratedFile(p.eventSubscription(`Subscribes${subscriptionName}`), content as string, `event subscription Subscribes${subscriptionName}`);
        });

        await Promise.all(writePromises);
    }

    private async generateIndividualEventHandlers(individualEventHandlers: any, aggregatePath: string): Promise<void> {
        const p = pathsFor(aggregatePath);
        const handlerEntries = Object.entries(individualEventHandlers).filter(([key]) => key.startsWith('specific-handler-'));

        const writePromises = handlerEntries.map(([key, content]) => {
            const handlerName = key.replace('specific-handler-', '');
            return FileWriter.writeGeneratedFile(p.eventHandler(handlerName), content as string, `specific event handler ${handlerName}`);
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
        const p = pathsFor(aggregatePath, aggregate.name);
        const baseHandlerPath = p.eventBaseHandler();
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
                await FileWriter.writeGeneratedFile(p.eventSubscription(className), code, `reference subscription ${className}`);
            } else if (key.startsWith('ref-handler-')) {
                const targetAggregate = key.replace('ref-handler-', '');
                const className = `${targetAggregate}DeletedEventHandler`;
                await FileWriter.writeGeneratedFile(p.eventHandler(className), code, `reference handler ${className}`);
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
