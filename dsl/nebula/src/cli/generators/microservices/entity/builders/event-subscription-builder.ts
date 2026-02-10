import { AggregateExt } from "../../../../types/ast-extensions.js";
import { getEvents, getReferences } from "../../../../utils/aggregate-helpers.js";

/**
 * Handles generation of the getEventSubscriptions() method for root entities.
 *
 * Event subscriptions are the mechanism by which aggregates register interest
 * in events from other aggregates. This builder generates the getEventSubscriptions()
 * override method that returns the set of event subscriptions for an aggregate.
 *
 * Responsibilities:
 * - Generate getEventSubscriptions() method for root entities
 * - Filter simple subscriptions (no conditions, no routing)
 * - Add ACTIVE state guard for subscriptions
 * - Generate calls to inter-invariant methods
 * - Generate simple subscription registrations
 */
export class EventSubscriptionBuilder {
    /**
     * Generates the getEventSubscriptions() method for a root entity
     */
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

        // Get reference constraints
        const references = getReferences(aggregate);
        const referenceConstraints = references?.constraints || [];

        // Filter for simple subscriptions (no conditions, no routing)
        const simpleSubscriptions = subscribedEvents.filter((sub: any) => {
            // Simple subscription: no conditions block or empty conditions, no routing
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        const hasInterInvariants = interInvariants.length > 0;
        const hasReferenceConstraints = referenceConstraints.length > 0;

        if (simpleSubscriptions.length === 0 && !hasInterInvariants && !hasReferenceConstraints) {
            return `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }`;
        }

        let methodBody = `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();`;

        // All subscriptions should only be added for ACTIVE aggregates
        const hasAnySubscriptions = hasInterInvariants || simpleSubscriptions.length > 0 || hasReferenceConstraints;

        if (hasAnySubscriptions) {
            methodBody += `\n        if (this.getState() == AggregateState.ACTIVE) {`;

            // Add inter-invariant method calls
            if (hasInterInvariants) {
                for (const invariant of interInvariants) {
                    const methodName = `interInvariant${this.toCamelCase(invariant.name)}`;
                    methodBody += `\n            ${methodName}(eventSubscriptions);`;
                }
            }

            // Add simple subscriptions (inside ACTIVE guard)
            if (simpleSubscriptions.length > 0) {
                for (const sub of simpleSubscriptions) {
                    // Handle different AST structures for event types
                    let eventTypeName = 'UnknownEvent';
                    if (typeof sub.eventType === 'string') {
                        eventTypeName = sub.eventType;
                    } else if ((sub.eventType as any)?.ref?.name) {
                        eventTypeName = (sub.eventType as any).ref.name;
                    } else if ((sub.eventType as any)?.$refText) {
                        eventTypeName = (sub.eventType as any).$refText;
                    } else if ((sub as any).eventType) {
                        // Fallback: try to extract from the raw eventType
                        eventTypeName = (sub as any).eventType;
                    }

                    // Extract aggregate name from event name (e.g., UpdateTopicEvent -> Topic, UserDeletedEvent -> User)
                    const eventNameWithoutPrefix = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
                    const subscriptionClassName = `${aggregate.name}Subscribes${eventNameWithoutPrefix}`;
                    methodBody += `\n            eventSubscriptions.add(new ${subscriptionClassName}());`;
                }
            }

            // Add reference constraint subscriptions (inside ACTIVE guard)
            if (hasReferenceConstraints) {
                for (const constraint of referenceConstraints) {
                    const targetAggregate = (constraint as any).targetAggregate;
                    const subscriptionClassName = `${aggregate.name}Subscribes${targetAggregate}Deleted`;
                    // Reference subscriptions need to pass 'this' to the constructor
                    methodBody += `\n            eventSubscriptions.add(new ${subscriptionClassName}(this));`;
                }
            }

            methodBody += `\n        }`;
        }

        methodBody += `\n        return eventSubscriptions;\n    }`;

        return methodBody;
    }

    /**
     * Converts snake_case_upper to PascalCase (e.g., COURSE_EXISTS -> CourseExists)
     */
    private toCamelCase(snakeCaseUpper: string): string {
        return snakeCaseUpper
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join('');
    }
}
