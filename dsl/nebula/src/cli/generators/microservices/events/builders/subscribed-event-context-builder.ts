import chalk from 'chalk';
import { Aggregate, SubscribedEvent } from "../../../../../language/generated/ast.js";
import { AggregateExt } from "../../../../types/ast-extensions.js";
import { GeneratorCapabilities } from "../../../common/generator-capabilities.js";
import { EventNameParser } from "../../../common/utils/event-name-parser.js";




export class SubscribedEventContextBuilder {
    constructor(private capabilities: GeneratorCapabilities) { }



    buildSubscribedEventContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        const lowerAggregate = aggregate.name.toLowerCase();
        const eventTypeName = (event as any).eventType || 'UnknownEvent';

        const subscriptionName = EventNameParser.generateSubscriptionName(aggregate.name, eventTypeName);


        let subscribingEntityName = event.sourceAggregate;
        if (!subscribingEntityName) {

            const inferredEntityName = EventNameParser.extractEntityName(eventTypeName);


            const entities = (aggregate as any).entities || [];
            const projectionEntity = entities.find((e: any) => {
                const aggregateRef = e.aggregateRef;
                return aggregateRef && aggregateRef.toLowerCase() === inferredEntityName.toLowerCase();
            });


            subscribingEntityName = projectionEntity ? projectionEntity.name : aggregate.name;
        }



        const subscribingEntityVariable = subscribingEntityName
            ? subscribingEntityName.charAt(0).toLowerCase() + subscribingEntityName.slice(1)
            : '';

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
            eventTypeName,
            subscribingEntityName
        );

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.events.subscribe`,
            subscriptionName,
            sourceAggregate: subscribingEntityName,
            sourceAggregateVariable: subscribingEntityVariable,
            sourceAggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate`,
            eventTypeName,
            eventTypePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.events`,
            conditions,
            subscriptionKeyAggregateIdExpr: aggregateIdExpr,
            subscriptionKeyVersionExpr: versionExpr
        };
    }



    buildInterInvariantSubscriptionContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        const aggregateName = aggregate.name;
        const eventTypeName = (event as any).eventType || 'UnknownEvent';
        const eventBaseName = EventNameParser.removeEventSuffix(eventTypeName);



        const interInvariantName = (event as any).interInvariantName || '';

        const fieldSuffix = interInvariantName
            ? interInvariantName.split('_').map((word: string) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join('')
            : '';
        const subscriptionName = `${aggregateName}Subscribes${eventBaseName}${fieldSuffix}`;


        const entityRef = this.extractEntityReferenceFromCondition(event, aggregate);
        if (!entityRef) {
            console.warn(chalk.yellow(`[WARN] Could not extract entity reference for inter-invariant subscription ${subscriptionName}`));
            return this.buildSubscribedEventContext(event, aggregate, options);
        }


        const cleanEntityName = entityRef.entityTypeName.replace(new RegExp(`^${aggregateName}`, 'i'), '');
        const lowerEntityName = cleanEntityName.charAt(0).toLowerCase() + cleanEntityName.slice(1);
        const aggregateIdField = `${lowerEntityName}AggregateId`;
        const versionField = `${lowerEntityName}Version`;

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.events.subscribe`,
            subscription: {
                capitalizedSubscriptionName: subscriptionName,
                entityTypeName: entityRef.entityTypeName,
                entityParamName: entityRef.fieldName,
                aggregateIdField: aggregateIdField,
                versionField: versionField,
                capitalizedAggregateIdField: aggregateIdField.charAt(0).toUpperCase() + aggregateIdField.slice(1),
                capitalizedVersionField: versionField.charAt(0).toUpperCase() + versionField.slice(1),
                eventTypeName: eventTypeName,
                eventType: eventBaseName
            },
            imports: [
                `import ${this.getBasePackage()}.ms.domain.event.Event;`,
                `import ${this.getBasePackage()}.ms.domain.event.EventSubscription;`,
                `import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.aggregate.${entityRef.entityTypeName};`,
                `import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.events.${eventTypeName};`,
                ''
            ]
        };
    }



    buildCustomEventHandlingContext(aggregate: AggregateExt, options: any): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();

        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);


        const eventsWithHandlers = allSubscribedEvents.filter((event: any) => !(event as any).isInterInvariant);

        const subscribedEvents = eventsWithHandlers.map(event => {
            const eventTypeName = (event as any).eventType || 'UnknownEvent';
            const handlerName = `${eventTypeName}Handler`;
            return { eventName: eventTypeName, handlerName };
        }) || [];

        const subscribedEventImports = eventsWithHandlers.map(event => {
            const eventTypeName = (event as any).eventType || 'UnknownEvent';
            const handlerName = `${eventTypeName}Handler`;

            return {
                handlerName,
                handlerPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
                eventName: eventTypeName,
                eventPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.events`
            };
        }) || [];

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling`,
            aggregateName,
            lowerAggregateName,
            coordinationPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.coordination.eventProcessing`,
            aggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`,
            subscribedEvents,
            subscribedEventImports
        };
    }



    buildEventHandlerContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();
        const eventTypeName = (event as any).eventType || 'UnknownEvent';
        const handlerName = `${eventTypeName}Handler`;

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
            handlerName,
            aggregateName,
            lowerAggregateName,
            eventTypeName,
            aggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`,
            aggregateCoordinationPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.coordination.eventProcessing`,
            eventTypePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.events`
        };
    }



    buildBaseEventHandlerContext(aggregate: AggregateExt, options: { projectName: string }): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
            aggregateName,
            lowerAggregateName,
            coordinationPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.coordination.eventProcessing`,
            aggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`
        };
    }







    private extractEntityReferenceFromCondition(event: SubscribedEvent, aggregate: AggregateExt): { fieldName: string, entityTypeName: string } | null {
        const conditions = event.conditions || [];
        if (conditions.length === 0) {
            return null;
        }

        const condition = conditions[0];
        let conditionText = '';

        if ((condition as any).condition?.$cstNode?.text) {
            conditionText = (condition as any).condition.$cstNode.text.trim();
        } else {
            return null;
        }


        const match = conditionText.match(/^(\w+)\./);
        if (!match) {
            return null;
        }

        const fieldName = match[1];


        const rootEntity: any = (aggregate as any).entities?.find((e: any) => e.isRoot);
        if (!rootEntity) {
            return null;
        }

        const property = rootEntity.properties.find((p: any) => p.name === fieldName);
        if (!property) {
            return null;
        }


        const entityTypeName = this.extractEntityTypeName(property);
        if (!entityTypeName) {
            return null;
        }

        return { fieldName, entityTypeName };
    }



    private extractEntityTypeName(property: any): string | null {
        const typeObj = property.type;
        if (!typeObj) {
            return null;
        }


        if ((typeObj.$type === 'ListType' || typeObj.$type === 'SetType') && typeObj.elementType) {
            const elementType = typeObj.elementType;

            if (elementType.$type === 'EntityType') {

                if (elementType.type?.ref?.name) {
                    return elementType.type.ref.name;
                }
                if (elementType.type?.$refText) {
                    return elementType.type.$refText;
                }
            }

            if (elementType.$refText) {
                return elementType.$refText;
            }
            if (elementType.type?.$refText) {
                return elementType.type.$refText;
            }
        }


        if (typeObj.$type === 'EntityType' && typeObj.type?.$refText) {
            return typeObj.type.$refText;
        }


        if (typeObj.type?.$refText) {
            return typeObj.type.$refText;
        }


        if (typeObj.collection) {
            if (typeObj.type?.type?.$refText) {
                return typeObj.type.type.$refText;
            }
            if (typeObj.type?.$refText) {
                return typeObj.type.$refText;
            }
        }

        return null;
    }







    private buildSubscriptionKeyExpressions(
        event: SubscribedEvent,
        aggregate: Aggregate,
        aggregateVariable: string,
        eventTypeName: string,
        subscribingEntityName?: string
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

        const aggregateName = aggregate.name;
        if (subscribingEntityName && subscribingEntityName !== aggregateName) {
            const cleanEntityName = subscribingEntityName.replace(new RegExp(`^${aggregateName}`, 'i'), '');
            if (cleanEntityName && cleanEntityName !== subscribingEntityName) {
                const lowerEntityName = cleanEntityName.charAt(0).toLowerCase() + cleanEntityName.slice(1);
                const aggregateIdField = `${lowerEntityName}AggregateId`;
                const versionField = `${lowerEntityName}Version`;
                const capAggId = aggregateIdField.charAt(0).toUpperCase() + aggregateIdField.slice(1);
                const capVersion = versionField.charAt(0).toUpperCase() + versionField.slice(1);

                return {
                    aggregateIdExpr: `${aggregateVariable}.get${capAggId}()`,
                    versionExpr: `${aggregateVariable}.get${capVersion}()`
                };
            }
        }

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







    private collectSubscribedEvents(aggregate: AggregateExt): any[] {
        const direct = aggregate.events?.subscribedEvents || [];
        const interInvariants = (aggregate.events as any)?.interInvariants || [];
        const interSubs = interInvariants.flatMap((ii: any) =>
            (ii?.subscribedEvents || []).map((sub: any) => ({ ...sub, isInterInvariant: true }))
        );
        const allSubscribed = [...direct, ...interSubs];
        return allSubscribed;
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







    private getBasePackage(): string {
        return this.capabilities.packageBuilder.buildCustomPackage('').split('.').slice(0, -1).join('.');
    }
}
