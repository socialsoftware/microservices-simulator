import { PublishedEvent, SubscribedEvent } from "../../../../language/generated/ast.js";
import { AggregateExt, TypeGuards } from "../../../types/ast-extensions.js";
import { GeneratorCapabilities, GeneratorCapabilitiesFactory } from "../../common/generator-capabilities.js";
import { EventContextBuilder } from "./builders/index.js";

export class EventGenerator {
    private capabilities: GeneratorCapabilities;
    private contextBuilder: EventContextBuilder;

    constructor(capabilities?: GeneratorCapabilities) {
        this.capabilities = capabilities || GeneratorCapabilitiesFactory.createEventCapabilities();
        this.contextBuilder = new EventContextBuilder(this.capabilities);
    }

    private loadTemplate(templatePath: string): string {
        return templatePath;
    }

    private renderTemplate(templatePath: string, context: any): string {
        return this.capabilities.templateRenderer.render(templatePath, context);
    }

    async generateEvents(aggregate: AggregateExt, options: any): Promise<any> {
        const result: any = {};

        const rootEntity = aggregate.entities.find((e: any) => TypeGuards.isRootEntity(e as any));
        if (!rootEntity) {
            return result;
        }

        try {
            if (aggregate.events?.subscribedEvents && aggregate.events.subscribedEvents.length > 0) {
                const eventHandlingCode = this.generateCustomEventHandling(aggregate, options);
                result['event-handling'] = eventHandlingCode;
            }

        } catch (error) {
            console.error(`Error in generateEvents for ${aggregate.name}:`, error);
        }

        return result;
    }

    generatePublishedEvent(event: PublishedEvent, aggregate: AggregateExt, options: { projectName: string }): string {
        const context = this.contextBuilder.buildPublishedEventContext(event, aggregate, options);
        const template = this.loadTemplate('events/published-event.hbs');
        return this.renderTemplate(template, context);
    }

    generateSubscribedEvent(event: SubscribedEvent, aggregate: AggregateExt, options: { projectName: string }): string {
        const isInterInvariant = (event as any).isInterInvariant;

        if (isInterInvariant) {
            const context = this.contextBuilder.buildInterInvariantSubscriptionContext(event, aggregate, options);
            const template = this.loadTemplate('events/inter-invariant-subscription.hbs');
            return this.renderTemplate(template, context);
        } else {
            const context = this.contextBuilder.buildSubscribedEventContext(event, aggregate, options);
            const template = this.loadTemplate('events/subscribed-event.hbs');
            return this.renderTemplate(template, context);
        }
    }

    generateEventHandler(event: SubscribedEvent, aggregate: AggregateExt, options: { projectName: string }): string {
        const context = this.contextBuilder.buildEventHandlerContext(event, aggregate, options);
        const template = this.loadTemplate('events/event-handler.hbs');
        return this.renderTemplate(template, context);
    }

    generateBaseEventHandler(aggregate: AggregateExt, options: { projectName: string }): string {
        const context = this.contextBuilder.buildBaseEventHandlerContext(aggregate, options);
        const template = this.loadTemplate('events/base-event-handler.hbs');
        return this.renderTemplate(template, context);
    }

    generateCustomEventHandling(aggregate: AggregateExt, options: { projectName: string }): string {
        const context = this.contextBuilder.buildCustomEventHandlingContext(aggregate, options);
        const template = this.loadTemplate('events/event-handling.hbs');
        return this.renderTemplate(template, context);
    }
}
