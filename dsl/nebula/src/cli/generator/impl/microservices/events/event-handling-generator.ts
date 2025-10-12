import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventContext } from "./event-types.js";

export class EventHandlingGenerator extends EventBaseGenerator {
    async generateEventHandling(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<string> {
        const context = this.buildEventHandlingContext(aggregate, rootEntity, options);
        const template = this.getEventHandlingTemplate();
        return this.renderTemplate(template, context);
    }

    private buildEventHandlingContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventSubscriptions = this.buildEventSubscriptions(aggregate, rootEntity, baseContext.aggregateName);
        const imports = this.buildEventHandlingImports(aggregate, options, eventSubscriptions);

        return {
            ...baseContext,
            imports,
            eventSubscriptions
        } as any;
    }

    private buildEventSubscriptions(aggregate: Aggregate, rootEntity: Entity, aggregateName: string): any[] {
        const eventTypes = ['Created', 'Updated', 'Deleted'];

        return eventTypes.map(eventType => {
            const variations = this.getEventNameVariations(eventType, aggregateName);
            return {
                eventType,
                ...variations,
                sourceAggregate: aggregateName,
                isExternal: false,
                priority: this.getEventPriority(eventType)
            };
        });
    }

    private buildEventHandlingImports(aggregate: Aggregate, options: EventGenerationOptions, eventSubscriptions: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const projectName = options?.projectName?.toLowerCase() || 'unknown';

        return [
            ...baseImports,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.microservices.${lowerAggregate}.aggregate.*;`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.microservices.${lowerAggregate}.service.*;`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.coordination.eventProcessing.*;`,
            'import org.slf4j.Logger;',
            'import org.slf4j.LoggerFactory;',
            'import java.util.concurrent.CompletableFuture;',
            ''
        ];
    }

    private getEventPriority(eventType: string): number {
        const priorities: { [key: string]: number } = {
            'Created': 1,
            'Updated': 2,
            'Deleted': 3
        };
        return priorities[eventType] || 0;
    }

    private getEventHandlingTemplate(): string {
        return this.loadTemplate('events/event-handling.hbs');
    }
}
