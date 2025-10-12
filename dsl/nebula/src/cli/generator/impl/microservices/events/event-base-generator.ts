import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../../base/orchestration-base.js";
import { EventGenerationOptions, EventContext } from "./event-types.js";

export abstract class EventBaseGenerator extends OrchestrationBase {
    protected createBaseEventContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventContext {
        const naming = this.createAggregateNaming(aggregate.name);
        const projectName = options?.projectName || 'unknown';
        const packageName = this.generatePackageName(projectName, aggregate.name, 'events');

        return {
            aggregateName: naming.original,
            capitalizedAggregate: naming.capitalized,
            lowerAggregate: naming.lower,
            packageName,
            rootEntity,
            projectName,
            imports: this.buildStandardImports(projectName, aggregate.name)
        };
    }

    protected buildEventProperties(rootEntity: Entity, eventType: string): any[] {
        return this.buildPropertyInfo(rootEntity);
    }

    protected buildBaseImports(aggregate: Aggregate, options: EventGenerationOptions): string[] {
        const projectName = options?.projectName || 'unknown';
        const baseImports = this.buildStandardImports(projectName, aggregate.name);
        const eventImports = [
            'import org.springframework.context.ApplicationEvent;',
            'import org.springframework.context.event.EventListener;',
            'import org.springframework.scheduling.annotation.Async;',
            'import org.springframework.transaction.event.TransactionalEventListener;',
            'import org.springframework.transaction.event.TransactionPhase;',
        ];

        return this.combineImports(baseImports, eventImports);
    }

    protected getEventNameVariations(eventName: string, aggregateName: string) {
        const capitalizedAggregate = this.capitalize(aggregateName);

        return {
            eventName,
            capitalizedEventName: this.capitalize(eventName),
            lowerEventName: eventName.toLowerCase(),
            fullEventName: `${capitalizedAggregate}${this.capitalize(eventName)}Event`,
            handlerName: `${eventName}Handler`,
            capitalizedHandlerName: `${this.capitalize(eventName)}Handler`,
            subscriptionName: `${eventName}Subscription`,
            capitalizedSubscriptionName: `${this.capitalize(eventName)}Subscription`
        };
    }
}
