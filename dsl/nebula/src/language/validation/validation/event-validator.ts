import type { ValidationAcceptor } from "langium";
import type { Aggregate, Entity, Events, EventField, PublishedEvent, SubscribedEvent } from "../../generated/ast.js";

export class EventValidator {
    checkSubscribedEvent(event: SubscribedEvent, accept: ValidationAcceptor): void {
        const published = event.eventType.ref as PublishedEvent | undefined;
        if (!published) {
            const name = event.eventType.$refText ?? "UnknownEvent";
            accept("error", `Subscribed event type '${name}' is not defined in this aggregate.`, {
                node: event,
                property: "eventType"
            });
            return;
        }

        const eventsContainer = event.$container as Events;
        const aggregate = eventsContainer.$container as Aggregate;

        const entities = (aggregate as any).entities as Entity[] | undefined;
        const rootEntity = entities ? entities.find(e => (e as any).isRoot) : undefined;

        const eventFields = new Map<string, EventField>();
        for (const field of published.fields) {
            eventFields.set(field.name, field);
        }

        for (const cond of event.conditions) {
            if (!cond.condition) continue;
            this.validateConditionExpression(cond.condition, eventFields, rootEntity, aggregate, event, accept);
        }

        if ((event as any).routingIdExpr) {
            this.validateConditionExpression((event as any).routingIdExpr, eventFields, rootEntity, aggregate, event, accept);
        }
        if ((event as any).routingVersionExpr) {
            this.validateConditionExpression((event as any).routingVersionExpr, eventFields, rootEntity, aggregate, event, accept);
        }
    }

    private validateConditionExpression(
        expr: any,
        eventFields: Map<string, EventField>,
        rootEntity: Entity | undefined,
        aggregate: Aggregate,
        event: SubscribedEvent,
        accept: ValidationAcceptor
    ): void {
        if (!expr) return;
        const type = expr.$type;

        if (type === 'BooleanExpression' || type === 'Comparison') {
            if (expr.left) {
                this.validateConditionExpression(expr.left, eventFields, rootEntity, aggregate, event, accept);
            }
            if (expr.right) {
                this.validateConditionExpression(expr.right, eventFields, rootEntity, aggregate, event, accept);
            }
            return;
        }

        if (type === 'PropertyChainExpression') {
            this.validatePropertyChain(expr, eventFields, rootEntity, aggregate, event, accept);
            return;
        }

        if (type === 'PropertyReference') {
            return;
        }

        if (expr.condition) {
            this.validateConditionExpression(expr.condition, eventFields, rootEntity, aggregate, event, accept);
        }
    }

    private validatePropertyChain(
        expr: any,
        eventFields: Map<string, EventField>,
        rootEntity: Entity | undefined,
        aggregate: Aggregate,
        event: SubscribedEvent,
        accept: ValidationAcceptor
    ): void {
        const headName = expr.head?.name;
        if (!headName) return;

        const segments: string[] = [];
        let current = expr;
        while (current) {
            if (current.member) {
                segments.unshift(current.member);
            }
            current = current.receiver;
        }

        if (headName === 'event') {
            const fieldName = segments[0];
            if (!fieldName) return;
            if (!eventFields.has(fieldName)) {
                const suggestions = Array.from(eventFields.keys())
                    .filter(n => n.toLowerCase().includes(fieldName.toLowerCase()) || fieldName.toLowerCase().includes(n.toLowerCase()))
                    .slice(0, 3);
                const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                accept("error", `Field '${fieldName}' does not exist on published event '${(event.eventType.ref as any)?.name ?? event.eventType.$refText}'.${suggestionText}`, {
                    node: event,
                    property: "eventType"
                });
            }
            return;
        }

        if (!rootEntity) return;
        if (headName === (aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1))) {
            this.validateAggregatePropertyPath(segments, rootEntity, aggregate, event, accept);
        }
    }

    private validateAggregatePropertyPath(
        segments: string[],
        rootEntity: Entity,
        aggregate: Aggregate,
        event: SubscribedEvent,
        accept: ValidationAcceptor
    ): void {
        let currentEntity: Entity | undefined = rootEntity;
        for (let i = 0; i < segments.length; i++) {
            const seg = segments[i];
            const props = currentEntity?.properties ?? [];
            const prop = props.find(p => p.name === seg);
            if (!prop) {
                const suggestions = props
                    .map(p => p.name)
                    .filter(n => n.toLowerCase().includes(seg.toLowerCase()) || seg.toLowerCase().includes(n.toLowerCase()))
                    .slice(0, 3);
                const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                accept("error", `Property '${seg}' not found on entity '${currentEntity?.name ?? aggregate.name}'.${suggestionText}`, {
                    node: event,
                    property: "eventType"
                });
                return;
            }

            if (i < segments.length - 1) {
                const propType: any = prop.type;
                if (propType?.$type === 'EntityType' && propType.type?.ref) {
                    currentEntity = propType.type.ref as Entity;
                } else {
                    accept("error", `Property '${seg}' on entity '${currentEntity.name}' is not an entity reference and cannot have nested properties.`, {
                        node: event,
                        property: "eventType"
                    });
                    return;
                }
            }
        }
    }
}

