import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventHandlerContext } from "./event-types.js";
import { EventNameParser } from "../../common/utils/event-name-parser.js";

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
        const eventHandlers = this.buildEventHandlers(aggregate, rootEntity, baseContext.aggregateName, options);
        const imports = this.buildEventHandlersImports(aggregate, options, eventHandlers);

        return {
            ...baseContext,
            eventHandlers,
            imports
        };
    }

    private buildEventHandlers(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, options: EventGenerationOptions): any[] {
        
        const events = (aggregate as any).events;
        if (!events || !events.subscribedEvents) {
            return [];
        }

        
        const simpleSubscriptions = events.subscribedEvents.filter((sub: any) => {
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        const projectName = (this as any).projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getEventBasePackage(options);

        return simpleSubscriptions.map((sub: any) => {
            const eventTypeName = sub.eventType || 'UnknownEvent';


            const entityName = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
            const handlerName = `${entityName}EventHandler`;
            
            return {
                eventType: EventNameParser.removeEventSuffix(eventTypeName),
                eventTypeName: eventTypeName,
                handlerName: handlerName,
                capitalizedHandlerName: handlerName,
                capitalizedEventName: eventTypeName,
                fullEventName: eventTypeName,
                eventTypePackage: `${basePackage}.${projectName}.events`,
                properties: this.buildEventProperties(rootEntity, EventNameParser.removeEventSuffix(eventTypeName))
            };
        });
    }

    private buildEventHandlersImports(aggregate: Aggregate, options: EventGenerationOptions, eventHandlers: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const projectName = options?.projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getEventBasePackage(options);

        const imports = [
            ...baseImports,
            `import ${basePackage}.ms.domain.event.EventHandler;`,
            `import ${basePackage}.ms.domain.event.Event;`,
            `import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${this.capitalize(aggregate.name)}Repository;`,
            `import ${basePackage}.${projectName}.coordination.eventProcessing.${this.capitalize(aggregate.name)}EventProcessing;`
        ];

        
        eventHandlers.forEach((handler: any) => {
            imports.push(`import ${handler.eventTypePackage}.${handler.eventTypeName};`);
        });

        imports.push('');
        return imports;
    }

    private async generateEventHandlerBase(context: EventHandlerContext): Promise<string> {
        const template = this.loadRawTemplate('events/event-handler-base.hbs');
        return this.renderTemplateFromString(template, context);
    }

    private async generateIndividualEventHandler(context: any): Promise<string> {
        const template = this.loadRawTemplate('events/individual-event-handler.hbs');
        return this.renderTemplateFromString(template, context);
    }
}