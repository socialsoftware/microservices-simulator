import { Aggregate, PublishedEvent, SubscribedEvent, EventField } from "../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../common/orchestration-base.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";

export class EventGenerator extends OrchestrationBase {

    async generateEvents(aggregate: Aggregate, options: any): Promise<any> {
        const result: any = {};

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            return result;
        }

        try {
            if (aggregate.events?.subscribedEvents && aggregate.events.subscribedEvents.length > 0) {
                const eventHandlingCode = this.generateCustomEventHandling(aggregate, options);
                result['event-handling'] = eventHandlingCode;
            }

        } catch (error) {
            console.error(`Error in generateEvents for ${aggregate.name}:`, error);
        }

        return result;
    }

    generatePublishedEvent(event: PublishedEvent, aggregate: Aggregate, options: { projectName: string }): string {
        const context = this.buildPublishedEventContext(event, aggregate, options);
        const template = this.loadTemplate('events/published-event.hbs');
        return this.renderTemplate(template, context);
    }

    generateSubscribedEvent(event: SubscribedEvent, aggregate: Aggregate, options: { projectName: string }): string {
        const context = this.buildSubscribedEventContext(event, aggregate, options);
        const template = this.loadTemplate('events/subscribed-event.hbs');
        return this.renderTemplate(template, context);
    }

    generateEventHandler(event: SubscribedEvent, aggregate: Aggregate, options: { projectName: string }): string {
        const context = this.buildEventHandlerContext(event, aggregate, options);
        const template = this.loadTemplate('events/event-handler.hbs');
        return this.renderTemplate(template, context);
    }

    generateBaseEventHandler(aggregate: Aggregate, options: { projectName: string }): string {
        const context = this.buildBaseEventHandlerContext(aggregate, options);
        const template = this.loadTemplate('events/base-event-handler.hbs');
        return this.renderTemplate(template, context);
    }

    private buildPublishedEventContext(event: PublishedEvent, aggregate: Aggregate, options: { projectName: string }): any {
        const eventName = event.name;
        const aggregateName = aggregate.name.toLowerCase();

        const fields = event.fields.map((field: EventField) => ({
            type: TypeResolver.resolveJavaType(field.type),
            name: field.name,
            capitalizedName: this.capitalize(field.name)
        }));

        const imports = this.generatePublishedEventImports(fields);

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName}.events.publish`,
            eventName,
            fields,
            imports
        };
    }

    private buildSubscribedEventContext(event: SubscribedEvent, aggregate: Aggregate, options: { projectName: string }): any {
        const aggregateName = aggregate.name.toLowerCase();
        const eventTypeName = event.eventType.ref?.name || event.eventType.$refText || 'UnknownEvent';

        const subscriptionName = `${aggregate.name}Subscribes${eventTypeName.replace('Event', '')}`;

        const subscribingEntityName = event.sourceAggregate;
        const subscribingEntityVariable = subscribingEntityName.toLowerCase();

        let eventSourceAggregate = 'unknown';
        const publishedEvent = event.eventType.ref as any;
        const eventsContainer = publishedEvent?.$container as any;
        const sourceAggregate = eventsContainer?.$container as Aggregate | undefined;
        if (sourceAggregate?.name) {
            eventSourceAggregate = sourceAggregate.name.toLowerCase();
        }

        const conditions = event.conditions?.map((condition: any) => {
            if (!condition.condition) {
                return { condition: 'true' };
            }
            return {
                condition: this.convertEventConditionToJava(condition.condition, subscribingEntityVariable, eventTypeName)
            };
        }).filter((c: any) => c.condition) || [];

        const { aggregateIdExpr, versionExpr } = this.buildSubscriptionKeyExpressions(
            event,
            aggregate,
            subscribingEntityVariable,
            eventTypeName
        );

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName}.events.subscribe`,
            subscriptionName,
            sourceAggregate: subscribingEntityName,
            sourceAggregateVariable: subscribingEntityVariable,
            sourceAggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName}.aggregate`,
            eventTypeName,
            eventTypePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${eventSourceAggregate}.events.publish`,
            conditions,
            subscriptionKeyAggregateIdExpr: aggregateIdExpr,
            subscriptionKeyVersionExpr: versionExpr
        };
    }

    private buildSubscriptionKeyExpressions(
        event: SubscribedEvent,
        aggregate: Aggregate,
        aggregateVariable: string,
        eventTypeName: string
    ): { aggregateIdExpr: string; versionExpr: string } {
        if ((event as any).routingIdExpr) {
            const idExpr = this.buildGetterChainFromRoutingExpression(
                (event as any).routingIdExpr,
                aggregateVariable,
                eventTypeName
            );
            const versionExprNode = (event as any).routingVersionExpr;
            const versionExpr = versionExprNode
                ? this.buildGetterChainFromRoutingExpression(versionExprNode, aggregateVariable, eventTypeName)
                : '0';

            return {
                aggregateIdExpr: idExpr,
                versionExpr
            };
        }

        const defaultExpr = {
            aggregateIdExpr: `${aggregateVariable}.getAggregateId()`,
            versionExpr: '0'
        };

        const rootEntity: any = (aggregate as any).entities?.find((e: any) => e.isRoot);
        if (!rootEntity || !rootEntity.properties) {
            return defaultExpr;
        }

        const candidates: {
            baseName: string;
            aggregateIdField: string;
            versionField: string;
        }[] = [];

        for (const prop of rootEntity.properties as any[]) {
            const propType = prop.type;
            if (propType?.$type === 'EntityType' && propType.type?.ref) {
                const nestedEntity: any = propType.type.ref;
                if (!nestedEntity?.properties) continue;

                const baseName: string = prop.name;
                const aggregateIdField = `${baseName}AggregateId`;
                const versionField = `${baseName}Version`;

                const hasAggregateIdField = nestedEntity.properties.some((p: any) => p.name === aggregateIdField);
                const hasVersionField = nestedEntity.properties.some((p: any) => p.name === versionField);

                if (hasAggregateIdField && hasVersionField) {
                    candidates.push({ baseName, aggregateIdField, versionField });
                }
            }
        }

        if (candidates.length === 0) {
            return defaultExpr;
        }

        const eventTypeLower = eventTypeName.toLowerCase();
        let selected = candidates[0];

        const matched = candidates.find(c => {
            const baseLower = c.baseName.toLowerCase();
            return eventTypeLower.includes(baseLower) || baseLower.includes(eventTypeLower);
        });

        if (matched) {
            selected = matched;
        }

        const capBase = selected.baseName.charAt(0).toUpperCase() + selected.baseName.slice(1);
        const capAggId = selected.aggregateIdField.charAt(0).toUpperCase() + selected.aggregateIdField.slice(1);
        const capVersion = selected.versionField.charAt(0).toUpperCase() + selected.versionField.slice(1);

        return {
            aggregateIdExpr: `${aggregateVariable}.get${capBase}().get${capAggId}()`,
            versionExpr: `${aggregateVariable}.get${capBase}().get${capVersion}()`
        };

        return defaultExpr;
    }

    private buildGetterChainFromRoutingExpression(
        expr: any,
        aggregateVariable: string,
        eventTypeName: string
    ): string {
        const text = expr?.$cstNode?.text?.trim();
        if (!text) {
            return this.convertEventConditionToJava(expr, aggregateVariable, eventTypeName);
        }

        if (/[<>=!&|+\-*/?]/.test(text)) {
            return this.convertEventConditionToJava(expr, aggregateVariable, eventTypeName);
        }

        const raw = text.replace(/^\(|\)$/g, '');
        const parts = raw.split('.');
        if (parts.length === 0) {
            return this.convertEventConditionToJava(expr, aggregateVariable, eventTypeName);
        }

        if (parts[0] === 'event') {
            let result = `((${eventTypeName})event)`;
            for (const segment of parts.slice(1)) {
                if (!segment) continue;
                const cap = segment.charAt(0).toUpperCase() + segment.slice(1);
                result += `.get${cap}()`;
            }
            return result;
        }

        if (parts[0] === aggregateVariable) {
            let result = aggregateVariable;
            for (const segment of parts.slice(1)) {
                if (!segment) continue;
                const cap = segment.charAt(0).toUpperCase() + segment.slice(1);
                result += `.get${cap}()`;
            }
            return result;
        }

        return this.convertEventConditionToJava(expr, aggregateVariable, eventTypeName);
    }

    private generatePublishedEventImports(fields: any[]): string {
        const imports = new Set<string>();

        fields.forEach(field => {
            if (field.type === 'LocalDateTime') {
                imports.add('import java.time.LocalDateTime;');
            } else if (field.type === 'BigDecimal') {
                imports.add('import java.math.BigDecimal;');
            } else if (field.type.startsWith('Set<')) {
                imports.add('import java.util.Set;');
            } else if (field.type.startsWith('List<')) {
                imports.add('import java.util.List;');
            }
        });

        return Array.from(imports).join('\n');
    }

    generateCustomEventHandling(aggregate: Aggregate, options: { projectName: string }): string {
        const context = this.buildCustomEventHandlingContext(aggregate, options);
        const template = this.loadTemplate('events/event-handling.hbs');
        return this.renderTemplate(template, context);
    }

    private buildCustomEventHandlingContext(aggregate: Aggregate, options: { projectName: string }): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();

        const subscribedEvents = aggregate.events?.subscribedEvents?.map(event => {
            const eventTypeName = event.eventType.ref?.name || event.eventType.$refText || 'UnknownEvent';
            const handlerName = `${eventTypeName}Handler`;

            return {
                eventName: eventTypeName,
                handlerName: handlerName
            };
        }) || [];

        const subscribedEventImports = aggregate.events?.subscribedEvents?.map(event => {
            const eventTypeName = event.eventType.ref?.name || event.eventType.$refText || 'UnknownEvent';
            const sourceAggregate = event.sourceAggregate;

            return {
                handlerName: `${eventTypeName}Handler`,
                handlerPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
                eventName: eventTypeName,
                eventPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${sourceAggregate.toLowerCase()}.events.publish`
            };
        }) || [];

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling`,
            aggregateName,
            lowerAggregateName,
            coordinationPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.coordination.eventProcessing`,
            aggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`,
            subscribedEvents,
            subscribedEventImports
        };
    }

    private buildEventHandlerContext(event: SubscribedEvent, aggregate: Aggregate, options: { projectName: string }): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();
        const eventTypeName = event.eventType.ref?.name || event.eventType.$refText || 'UnknownEvent';
        const sourceAggregate = event.sourceAggregate;

        const handlerName = `${eventTypeName}Handler`;

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
            handlerName,
            aggregateName,
            lowerAggregateName,
            eventTypeName,
            aggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`,
            aggregateCoordinationPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.coordination.eventProcessing`,
            eventTypePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${sourceAggregate.toLowerCase()}.events.publish`
        };
    }

    private buildBaseEventHandlerContext(aggregate: Aggregate, options: { projectName: string }): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
            aggregateName,
            lowerAggregateName,
            coordinationPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.coordination.eventProcessing`,
            aggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`
        };
    }

    private convertEventConditionToJava(expression: any, entityVariable: string, eventTypeName: string): string {
        if (!expression) {
            return "true";
        }

        const text = expression.$cstNode?.text?.trim();
        if (!text) {
            return this.convertExpressionASTToJava(expression, entityVariable, eventTypeName);
        }

        const chainRegex = /\b(event|[a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)/g;

        const rewritten = text.replace(chainRegex, (_match: string, base: string, tail: string) => {
            const segments = tail.split(".");
            if (base === "event") {
                let result = `((${eventTypeName})event)`;
                for (const seg of segments) {
                    if (!seg) continue;
                    const cap = seg.charAt(0).toUpperCase() + seg.slice(1);
                    result += `.get${cap}()`;
                }
                return result;
            }

            if (base === entityVariable) {
                let result = entityVariable;
                for (const seg of segments) {
                    if (!seg) continue;
                    const cap = seg.charAt(0).toUpperCase() + seg.slice(1);
                    result += `.get${cap}()`;
                }
                return result;
            }

            return `${base}.${tail}`;
        });

        return rewritten;
    }

    private convertExpressionASTToJava(expression: any, entityVariable: string, eventTypeName: string): string {
        if (!expression) {
            return 'true';
        }

        const type = expression.$type;

        if (type === 'BooleanExpression') {
            const left = this.convertExpressionASTToJava(expression.left, entityVariable, eventTypeName);
            if (expression.right && expression.op) {
                const right = this.convertExpressionASTToJava(expression.right, entityVariable, eventTypeName);
                const op = expression.op === '&&' || expression.op === 'AND' ? '&&' : '||';
                return `${left} ${op} ${right}`;
            }
            return left;
        }

        if (type === 'Comparison') {
            const left = this.convertExpressionASTToJava(expression.left, entityVariable, eventTypeName);
            if (expression.right && expression.op) {
                const right = this.convertExpressionASTToJava(expression.right, entityVariable, eventTypeName);
                if (expression.op === '==') {
                    if (this.isPropertyAccess(expression.left)) {
                        return `${left}.equals(${right})`;
                    }
                }
                return `${left} ${expression.op} ${right}`;
            }
            return left;
        }

        if (type === 'PropertyChainExpression') {
            return this.convertPropertyChainToJava(expression, entityVariable, eventTypeName);
        }

        if (type === 'PropertyReference') {
            const name = expression.name;
            if (name === 'event') {
                return `((${eventTypeName})event)`;
            }
            if (name === entityVariable) {
                return entityVariable;
            }
            const capitalized = name.charAt(0).toUpperCase() + name.slice(1);
            return `this.get${capitalized}()`;
        }

        if (type === 'LiteralExpression') {
            return expression.value || 'null';
        }

        return 'true';
    }

    private convertPropertyChainToJava(expression: any, entityVariable: string, eventTypeName: string): string {
        const head = expression.head;
        let result = '';

        if (head.name === 'event') {
            result = `((${eventTypeName})event)`;
        } else if (head.name === entityVariable) {
            result = entityVariable;
        } else {
            const capitalized = head.name.charAt(0).toUpperCase() + head.name.slice(1);
            result = `this.get${capitalized}()`;
        }

        let current = expression;
        while (current && (current.receiver || current.$type === 'MethodCall' || current.$type === 'PropertyAccess')) {
            if (current.$type === 'MethodCall') {
                const args = current.arguments?.map((arg: any) =>
                    this.convertExpressionASTToJava(arg, entityVariable, eventTypeName)
                ).join(', ') || '';
                result += `.${current.method}(${args})`;
            } else if (current.$type === 'PropertyAccess') {
                const capitalized = current.member.charAt(0).toUpperCase() + current.member.slice(1);
                result += `.get${capitalized}()`;
            }
            current = current.receiver;
        }

        return result;
    }

    private isPropertyAccess(expression: any): boolean {
        return expression?.$type === 'PropertyReference' || expression?.$type === 'PropertyChainExpression';
    }
}