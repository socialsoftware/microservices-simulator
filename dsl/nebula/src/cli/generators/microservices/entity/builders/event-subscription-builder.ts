import { AggregateExt } from "../../../../types/ast-extensions.js";
import { getEvents } from "../../../../utils/aggregate-helpers.js";
import { EventNameParser } from "../../../common/utils/event-name-parser.js";



export class EventSubscriptionBuilder {


    generateEventSubscriptionsMethod(aggregate: AggregateExt | undefined): string {
        if (!aggregate) {
            return `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }`;
        }

        const events = getEvents(aggregate);
        const subscribedEvents = events?.subscribedEvents || [];
        const interInvariants = (events as any)?.interInvariants || [];

        const simpleSubscriptions = subscribedEvents.filter((sub: any) => {
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        const hasInterInvariants = interInvariants.length > 0;

        if (simpleSubscriptions.length === 0 && !hasInterInvariants) {
            return `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }`;
        }

        let methodBody = `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {`;

        if (hasInterInvariants) {
            for (const invariant of interInvariants) {
                const methodName = `interInvariant${this.toCamelCase(invariant.name)}`;
                methodBody += `\n            ${methodName}(eventSubscriptions);`;
            }
        }

        if (simpleSubscriptions.length > 0) {
            for (const sub of simpleSubscriptions) {
                let eventTypeName = 'UnknownEvent';
                if (typeof sub.eventType === 'string') {
                    eventTypeName = sub.eventType;
                } else if ((sub.eventType as any)?.ref?.name) {
                    eventTypeName = (sub.eventType as any).ref.name;
                } else if ((sub.eventType as any)?.$refText) {
                    eventTypeName = (sub.eventType as any).$refText;
                } else if ((sub as any).eventType) {
                    eventTypeName = (sub as any).eventType;
                }

                const eventNameWithoutSuffix = EventNameParser.removeEventSuffix(eventTypeName);
                const subscriptionClassName = `${aggregate.name}Subscribes${eventNameWithoutSuffix}`;
                methodBody += `\n            eventSubscriptions.add(new ${subscriptionClassName}());`;
            }
        }

        methodBody += `\n        }\n        return eventSubscriptions;\n    }`;

        return methodBody;
    }

    

    private toCamelCase(snakeCaseUpper: string): string {
        return snakeCaseUpper
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join('');
    }
}
