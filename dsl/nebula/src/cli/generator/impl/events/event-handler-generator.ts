import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventHandlerContext } from "./event-types.js";

export class EventHandlerGenerator extends EventBaseGenerator {
    async generateEventHandlers(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};
        const context = this.buildEventHandlersContext(aggregate, rootEntity, options);

        const baseContext = {
            ...context,
            isAbstract: true
        };
        results[`event-handler-${context.aggregateName}EventHandler`] = await this.generateEventHandlerBase(baseContext);

        for (const handler of context.eventHandlers) {
            const handlerContext = {
                ...context,
                handler
            };
            results[`event-handler-${handler.handlerName}`] = await this.generateIndividualEventHandler(handlerContext);
        }

        return results;
    }

    private buildEventHandlersContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventHandlerContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventHandlers = this.buildEventHandlers(aggregate, rootEntity, baseContext.aggregateName);
        const imports = this.buildEventHandlersImports(aggregate, options, eventHandlers);

        return {
            ...baseContext,
            eventHandlers,
            imports
        };
    }

    private buildEventHandlers(aggregate: Aggregate, rootEntity: Entity, aggregateName: string): any[] {
        const eventTypes = ['Created', 'Updated', 'Deleted'];

        return eventTypes.map(eventType => {
            const variations = this.getEventNameVariations(eventType, aggregateName);
            return {
                eventType,
                ...variations,
                properties: this.buildEventProperties(rootEntity, eventType)
            };
        });
    }

    private buildEventHandlersImports(aggregate: Aggregate, options: EventGenerationOptions, eventHandlers: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const projectName = options?.projectName?.toLowerCase() || 'unknown';

        return [
            ...baseImports,
            `import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;`,
            `import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.microservices.${lowerAggregate}.aggregate.*;`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.microservices.${lowerAggregate}.service.*;`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.coordination.eventProcessing.${aggregate.name}EventProcessing;`,
            'import java.util.Set;',
            'import java.util.stream.Collectors;',
            ''
        ];
    }

    private async generateEventHandlerBase(context: EventHandlerContext): Promise<string> {
        const template = this.loadTemplate('events/event-handler-base.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateIndividualEventHandler(context: any): Promise<string> {
        const template = this.loadTemplate('events/individual-event-handler.hbs');
        return this.renderTemplate(template, context);
    }
}