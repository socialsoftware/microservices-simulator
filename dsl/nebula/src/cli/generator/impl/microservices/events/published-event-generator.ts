import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, PublishedEventContext } from "./event-types.js";

export class PublishedEventGenerator extends EventBaseGenerator {
    async generatePublishedEvents(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};
        const context = this.buildPublishedEventsContext(aggregate, rootEntity, options);

        for (const event of context.publishedEvents) {
            const eventContext = {
                ...context,
                event
            };
            results[`published-event-${event.eventName}`] = await this.generateIndividualPublishedEvent(eventContext);
        }

        return results;
    }

    private buildPublishedEventsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): PublishedEventContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const publishedEvents = this.buildPublishedEvents(rootEntity, baseContext.aggregateName);
        const imports = this.buildPublishedEventsImports(aggregate, options, publishedEvents);

        return {
            ...baseContext,
            publishedEvents,
            imports
        };
    }

    private buildPublishedEvents(rootEntity: Entity, aggregateName: string): any[] {
        const eventTypes = ['Created', 'Updated', 'Deleted'];

        return eventTypes.map(eventType => {
            const variations = this.getEventNameVariations(eventType, aggregateName);
            return {
                eventType,
                ...variations,
                properties: this.buildEventProperties(rootEntity, eventType),
                timestamp: new Date().toISOString()
            };
        });
    }

    private buildPublishedEventsImports(aggregate: Aggregate, options: EventGenerationOptions, publishedEvents: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);

        return [
            ...baseImports,
            'import java.time.LocalDateTime;',
            'import java.io.Serializable;',
            'import com.fasterxml.jackson.annotation.JsonFormat;',
            ''
        ];
    }

    private async generateIndividualPublishedEvent(context: any): Promise<string> {
        const template = this.loadTemplate('events/published-event.hbs');
        return this.renderTemplate(template, context);
    }
}
