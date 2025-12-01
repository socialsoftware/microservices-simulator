import type { ValidationAcceptor } from "langium";
import type { Aggregate, Entity, Events, EventField, PublishedEvent, SubscribedEvent } from "../../generated/ast.js";

export class EventValidator {
    checkSubscribedEvent(event: SubscribedEvent, accept: ValidationAcceptor): void {
        const published = event.eventType.ref as PublishedEvent | undefined;
        if (!published) {
            return;
        }

        const eventsContainer = event.$container as Events;
        const aggregate = eventsContainer.$container as Aggregate;

        const entities = (aggregate as any).entities as Entity[] | undefined;
        const rootEntity = entities ? entities.find(e => (e as any).isRoot) : undefined;
        const baseEntity = rootEntity ?? (entities && entities[0]) ?? undefined;

        if (event.sourceAggregate) {
            const validSourceNames = [
                aggregate.name,
                ...(entities?.map(e => e.name) ?? [])
            ];
            if (!validSourceNames.includes(event.sourceAggregate)) {
                const candidates = validSourceNames
                    .filter(name =>
                        name.toLowerCase().includes(event.sourceAggregate.toLowerCase()) ||
                        event.sourceAggregate.toLowerCase().includes(name.toLowerCase())
                    )
                    .slice(0, 3);
                const suggestionText = candidates.length > 0 ? ` Did you mean: ${candidates.join(', ')}?` : '';
                accept("error", `Source aggregate '${event.sourceAggregate}' is not a valid entity or root of aggregate '${aggregate.name}'.${suggestionText}`, {
                    node: event,
                    property: "sourceAggregate"
                });
            }
        }

        const eventFields = new Map<string, EventField>();
        for (const field of published.fields) {
            eventFields.set(field.name, field);
        }

        if (event.$cstNode?.text) {
            const fullText = event.$cstNode.text.trim();
            this.validateEventFieldUsages(fullText, eventFields, event, accept);
            if (baseEntity) {
                const aggregateVarName = aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1);
                this.validateAggregatePropertyPathUsages(fullText, aggregateVarName, baseEntity, aggregate, event, accept);
            }
        }

        for (const cond of event.conditions) {
            if (!cond.condition) continue;
            this.validateConditionExpression(cond.condition, eventFields, baseEntity, aggregate, event, accept);
        }

        if ((event as any).routingIdExpr) {
            this.validateConditionExpression((event as any).routingIdExpr, eventFields, baseEntity, aggregate, event, accept);
        }
        if ((event as any).routingVersionExpr) {
            this.validateConditionExpression((event as any).routingVersionExpr, eventFields, baseEntity, aggregate, event, accept);
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

        if ((expr as any).expression) {
            this.validateConditionExpression((expr as any).expression, eventFields, rootEntity, aggregate, event, accept);
        }

        if (expr.condition) {
            this.validateConditionExpression(expr.condition, eventFields, rootEntity, aggregate, event, accept);
        }
    }

    private validateEventFieldUsages(
        text: string,
        eventFields: Map<string, EventField>,
        event: SubscribedEvent,
        accept: ValidationAcceptor
    ): void {
        const regex = /\bevent\.(\w+)/g;
        let match: RegExpExecArray | null;
        const seen = new Set<string>();

        while ((match = regex.exec(text)) !== null) {
            const fieldName = match[1];
            if (seen.has(fieldName)) continue;
            seen.add(fieldName);

            if (!eventFields.has(fieldName)) {
                const suggestions = Array.from(eventFields.keys())
                    .filter(n => n.toLowerCase().includes(fieldName.toLowerCase()) || fieldName.toLowerCase().includes(n.toLowerCase()))
                    .slice(0, 3);
                const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                const publishedName = (event.eventType.ref as any)?.name ?? event.eventType.$refText;
                accept("error", `Field '${fieldName}' does not exist on published event '${publishedName}'.${suggestionText}`, {
                    node: event,
                    property: "eventType"
                });
            }
        }
    }

    private validateAggregatePropertyPathUsages(
        text: string,
        aggregateVarName: string,
        rootEntity: Entity,
        aggregate: Aggregate,
        event: SubscribedEvent,
        accept: ValidationAcceptor
    ): void {
        const regex = new RegExp(`\\b${aggregateVarName}\\.(\\w+(?:\\.\\w+)*)`, 'g');
        let match: RegExpExecArray | null;
        const seen = new Set<string>();

        while ((match = regex.exec(text)) !== null) {
            const fullPath = match[1];
            if (seen.has(fullPath)) continue;
            seen.add(fullPath);

            const segments = fullPath.split('.');
            let currentEntity: Entity | undefined = rootEntity;

            for (let i = 0; i < segments.length; i++) {
                const seg = segments[i];
                if (!currentEntity) break;

                const props = currentEntity.properties ?? [];
                const prop = props.find(p => p.name === seg);

                if (!prop) {
                    const suggestions = props
                        .map(p => p.name)
                        .filter(n => n.toLowerCase().includes(seg.toLowerCase()) || seg.toLowerCase().includes(n.toLowerCase()))
                        .slice(0, 3);
                    const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                    accept("error", `Property '${seg}' not found on entity '${currentEntity.name}'.${suggestionText}`, {
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

