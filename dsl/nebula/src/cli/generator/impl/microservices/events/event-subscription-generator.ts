import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventSubscriptionContext } from "./event-types.js";

export class EventSubscriptionGenerator extends EventBaseGenerator {
    async generateEventSubscriptions(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};
        const context = this.buildEventSubscriptionsContext(aggregate, rootEntity, options);

        for (const subscription of context.eventSubscriptions) {
            const subscriptionContext = {
                ...context,
                subscription
            };
            results[`event-subscription-${subscription.eventType}`] = await this.generateIndividualEventSubscription(subscriptionContext);
        }

        return results;
    }

    private buildEventSubscriptionsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventSubscriptionContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventSubscriptions = this.buildEventSubscriptions(aggregate, rootEntity, baseContext.aggregateName);
        const imports = this.buildEventSubscriptionsImports(aggregate, options, eventSubscriptions);

        return {
            ...baseContext,
            eventSubscriptions,
            imports
        };
    }

    private buildEventSubscriptions(aggregate: Aggregate, rootEntity: Entity, aggregateName: string): any[] {
        const eventTypes = ['Created', 'Updated', 'Deleted'];

        return eventTypes.map(eventType => {
            const variations = this.getEventNameVariations(eventType, aggregateName);
            return {
                eventType,
                ...variations,
                sourceAggregate: aggregateName,
                targetAggregate: aggregateName,
                isAsync: true,
                transactionPhase: 'AFTER_COMMIT'
            };
        });
    }

    private buildEventSubscriptionsImports(aggregate: Aggregate, options: EventGenerationOptions, eventSubscriptions: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const projectName = options?.projectName?.toLowerCase() || 'unknown';

        return [
            ...baseImports,
            `import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.microservices.${lowerAggregate}.aggregate.*;`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName}.microservices.${lowerAggregate}.service.*;`,
            'import org.springframework.scheduling.annotation.Async;',
            'import org.springframework.transaction.event.TransactionPhase;',
            ''
        ];
    }

    private async generateIndividualEventSubscription(context: any): Promise<string> {
        const template = this.loadTemplate('events/event-subscription.hbs');
        return this.renderTemplate(template, context);
    }
}
