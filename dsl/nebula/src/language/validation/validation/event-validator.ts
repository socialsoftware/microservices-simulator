import type { ValidationAcceptor } from "langium";
import type { Aggregate, Entity, EventField, PublishedEvent, SubscribedEvent } from "../../generated/ast.js";

export class EventValidator {
    checkSubscribedEvent(event: SubscribedEvent, accept: ValidationAcceptor): void {
        const aggregate = this.resolveAggregate(event);
        if (!aggregate) return;

        const aggregateElements = aggregate.aggregateElements || [];
        const entities = aggregateElements.filter((e: any) => e.$type === 'Entity') as Entity[];
        const rootEntity = entities.find(e => (e as any).isRoot);
        const baseEntity = rootEntity ?? entities[0];

        if (!event.sourceAggregate) {
            // Subscriptions without a source aggregate (e.g., inter-invariants) skip source validation
            return;
        }

        const validSourceNames = [
            aggregate.name,
            ...entities.map(e => e.name)
        ].filter(Boolean) as string[];

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

        const published = event.eventType.ref as PublishedEvent | undefined;
        const eventFields = new Map<string, EventField>();
        if (published) {
            for (const field of published.fields) {
                eventFields.set(field.name, field);
            }
        }

        if (baseEntity) {
            const aggregateVarName = aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1);

            if ((event as any).routingIdExpr) {
                const routingIdExpr = (event as any).routingIdExpr;
                if (routingIdExpr.$cstNode?.text) {
                    const text = routingIdExpr.$cstNode.text.trim();
                    this.validateAggregatePropertyPathUsages(text, aggregateVarName, baseEntity, aggregate, routingIdExpr, accept);
                } else {
                    this.validateConditionExpression(routingIdExpr, eventFields, baseEntity, aggregate, event, accept);
                }
            }
            if ((event as any).routingVersionExpr) {
                const routingVersionExpr = (event as any).routingVersionExpr;
                if (routingVersionExpr.$cstNode?.text) {
                    const text = routingVersionExpr.$cstNode.text.trim();
                    this.validateAggregatePropertyPathUsages(text, aggregateVarName, baseEntity, aggregate, routingVersionExpr, accept);
                } else {
                    this.validateConditionExpression(routingVersionExpr, eventFields, baseEntity, aggregate, event, accept);
                }
            }
        }

        if (published && event.$cstNode?.text) {
            const fullText = event.$cstNode.text.trim();
            this.validateEventFieldUsages(fullText, eventFields, event, accept);
        }

        for (const cond of event.conditions) {
            if (!cond.condition) continue;
            this.validateConditionExpression(cond.condition, eventFields, baseEntity, aggregate, event, accept);
        }
    }

    private resolveAggregate(node: any): Aggregate | undefined {
        let current: any = node;
        while (current) {
            if (current.$container && current.$container.$type === 'Aggregate') {
                return current.$container as Aggregate;
            }
            current = current.$container;
        }
        return undefined;
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

        if (type === 'ParenthesizedExpression') {
            if ((expr as any).expression) {
                this.validateConditionExpression((expr as any).expression, eventFields, rootEntity, aggregate, event, accept);
            }
            return;
        }

        if (type === 'PropertyChainExpression' || type === 'PropertyAccess' || type === 'MethodCall') {
            this.validatePropertyChain(expr, eventFields, rootEntity, aggregate, event, accept);
            return;
        }

        if (type === 'PropertyReference') {
            return;
        }

        if (expr.$cstNode?.text && rootEntity) {
            const text = expr.$cstNode.text.trim();
            const aggregateVarName = aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1);
            if (new RegExp(`\\b${aggregateVarName}\\.`).test(text)) {
                this.validateAggregatePropertyPathUsages(text, aggregateVarName, rootEntity, aggregate, expr, accept);
            }
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
        errorNode: any,
        accept: ValidationAcceptor
    ): void {
        const cleanedText = text.replace(/^\(+|\)+$/g, '');
        const regex = new RegExp(`\\b${aggregateVarName}\\.(\\w+(?:\\.\\w+)*)`, 'g');
        let match: RegExpExecArray | null;
        const seen = new Set<string>();

        while ((match = regex.exec(cleanedText)) !== null) {
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
                        .map(p => ({ name: p.name, similarity: this.stringSimilarity(seg.toLowerCase(), p.name.toLowerCase()) }))
                        .filter(s => s.similarity > 0.5)
                        .sort((a, b) => b.similarity - a.similarity)
                        .slice(0, 3)
                        .map(s => s.name);
                    const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                    accept("error", `Property '${seg}' not found on entity '${currentEntity.name}'.${suggestionText}`, {
                        node: errorNode,
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
                            node: errorNode,
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
        let headName: string | undefined;
        const segments: string[] = [];

        if (expr.head) {
            headName = expr.head.name;
        }

        let current: any = expr;
        while (current) {
            if (current.member) {
                segments.unshift(current.member);
            }
            if (!headName && current.$type === 'PropertyReference' && current.name) {
                headName = current.name;
                break;
            }
            current = current.receiver;
            if (current && current.$type === 'PropertyReference') {
                if (!headName && current.name) {
                    headName = current.name;
                }
                break;
            }
        }

        if (!headName) return;

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

    private stringSimilarity(s1: string, s2: string): number {
        if (s1 === s2) return 1;
        if (s1.length === 0 || s2.length === 0) return 0;

        const longer = s1.length > s2.length ? s1 : s2;
        const shorter = s1.length > s2.length ? s2 : s1;

        let matches = 0;
        for (let i = 0; i < shorter.length; i++) {
            if (longer.includes(shorter[i])) {
                matches++;
            }
        }

        const matchRatio = matches / longer.length;
        const lengthPenalty = 1 - Math.abs(s1.length - s2.length) / Math.max(s1.length, s2.length);

        let commonPrefix = 0;
        for (let i = 0; i < Math.min(s1.length, s2.length, 4); i++) {
            if (s1[i] === s2[i]) commonPrefix++;
            else break;
        }
        const prefixBonus = commonPrefix * 0.1;

        return Math.min(1, matchRatio * 0.5 + lengthPenalty * 0.3 + prefixBonus);
    }
}

