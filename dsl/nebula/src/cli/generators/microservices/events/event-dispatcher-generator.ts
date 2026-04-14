import chalk from 'chalk';
import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventContext } from "./event-types.js";
import { EventNameParser } from "../../common/utils/event-name-parser.js";

export class EventHandlingGenerator extends EventBaseGenerator {
    async generateEventHandling(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<string> {
        const context = this.buildEventHandlingContext(aggregate, rootEntity, options);
        const template = this.getEventHandlingTemplate();
        return this.renderTemplateFromString(template, context);
    }

    private buildEventHandlingContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventSubscriptions = this.buildEventSubscriptions(aggregate, rootEntity, baseContext.aggregateName, options);
        const imports = this.buildEventHandlingImports(aggregate, options, eventSubscriptions);
        const projectName = options?.projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getEventBasePackage(options);

        return {
            ...baseContext,
            packageName: `${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.events.handling`,
            coordinationPackage: `${basePackage}.${projectName}.coordination.eventProcessing`,
            aggregatePackage: `${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.aggregate`,
            lowerAggregateName: aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1),
            imports: imports.join('\n'),
            subscribedEvents: eventSubscriptions,
            subscribedEventImports: eventSubscriptions.map((sub: any) => ({
                handlerPackage: sub.handlerPackage,
                handlerName: sub.handlerName,
                eventPackage: sub.eventPackage,
                eventName: sub.eventName
            }))
        } as any;
    }

    private buildEventSubscriptions(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, options: EventGenerationOptions): any[] {
        
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


            const entityName = EventNameParser.extractEntityName(eventTypeName);


            let sourceAggregateName = 'unknown';

            if (sub.sourceAggregate) {
                sourceAggregateName = sub.sourceAggregate.toLowerCase();
            }

            else if (options?.allAggregates && options.allAggregates.length > 0) {
                const found = this.findEventPublisher(eventTypeName, options.allAggregates);
                if (found) {
                    sourceAggregateName = found;
                } else {
                    console.warn(chalk.yellow(`[WARN] Could not find publisher aggregate for event ${eventTypeName}`));

                    sourceAggregateName = entityName.toLowerCase();
                }
            }

            else {


                sourceAggregateName = entityName.toLowerCase();
            }

            const handlerName = `${entityName}EventHandler`;

            return {
                eventName: eventTypeName,
                handlerName: handlerName,
                eventPackage: `${basePackage}.${projectName}.events`,
                handlerPackage: `${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.events.handling.handlers`,
                sourceAggregate: sourceAggregateName,
                isExternal: sourceAggregateName !== aggregateName.toLowerCase()
            };
        });
    }

    private buildEventHandlingImports(aggregate: Aggregate, options: EventGenerationOptions, eventSubscriptions: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const projectName = options?.projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getEventBasePackage(options);

        const imports = [
            ...baseImports,
            `import ${basePackage}.ms.domain.event.EventApplicationService;`,
            `import ${basePackage}.${projectName}.coordination.eventProcessing.${this.capitalize(aggregate.name)}EventProcessing;`,
            `import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${this.capitalize(aggregate.name)}Repository;`
        ];

        
        eventSubscriptions.forEach((sub: any) => {
            imports.push(`import ${sub.eventPackage}.${sub.eventName};`);
            imports.push(`import ${sub.handlerPackage}.${sub.handlerName};`);
        });

        return imports;
    }

    private getEventHandlingTemplate(): string {
        return this.loadRawTemplate('events/event-handling.hbs');
    }
}
