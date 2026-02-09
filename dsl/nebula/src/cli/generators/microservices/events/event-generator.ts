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
        const isInterInvariant = (event as any).isInterInvariant;

        if (isInterInvariant) {
            const context = this.buildInterInvariantSubscriptionContext(event, aggregate, options);
            const template = this.loadTemplate('events/inter-invariant-subscription.hbs');
            return this.renderTemplate(template, context);
        } else {
            const context = this.buildSubscribedEventContext(event, aggregate, options);
            const template = this.loadTemplate('events/subscribed-event.hbs');
            return this.renderTemplate(template, context);
        }
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

    private buildSubscribedEventContext(event: SubscribedEvent, aggregate: Aggregate, options: any): any {
        const aggregateName = aggregate.name.toLowerCase();
        const eventTypeName = (event as any).eventType || 'UnknownEvent';

        const subscriptionName = `${aggregate.name}Subscribes${eventTypeName.replace('Event', '')}`;

        // Infer source aggregate if not explicitly provided
        let subscribingEntityName = event.sourceAggregate;
        if (!subscribingEntityName) {
            // Extract entity name from event (e.g., TopicDeletedEvent -> Topic)
            const inferredEntityName = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');

            // Try to find a projection entity that references this entity
            const entities = (aggregate as any).entities || [];
            const projectionEntity = entities.find((e: any) => {
                const aggregateRef = e.aggregateRef;
                return aggregateRef && aggregateRef.toLowerCase() === inferredEntityName.toLowerCase();
            });

            // Use projection entity if found, otherwise use root entity
            subscribingEntityName = projectionEntity ? projectionEntity.name : aggregate.name;
        }

        // use lower-camel case so multi-word entity names (e.g., AnswerQuiz) keep
        // their internal capitalization and still match routing expressions
        const subscribingEntityVariable = subscribingEntityName
            ? subscribingEntityName.charAt(0).toLowerCase() + subscribingEntityName.slice(1)
            : '';

        // Determine the publishing aggregate from the event
        let eventSourceAggregate = aggregateName;  // default to current aggregate

        // PRIORITY 1: Try AST reference (works for custom events with explicit references)
        const publishedEvent = (event as any).eventType?.ref;
        const eventsContainer = publishedEvent?.$container;
        const sourceAggregate = eventsContainer?.$container;
        if (sourceAggregate?.name) {
            eventSourceAggregate = sourceAggregate.name.toLowerCase();
        } else if (event.sourceAggregate) {
            eventSourceAggregate = event.sourceAggregate.toLowerCase();
        }
        // PRIORITY 2: Search all aggregates for event publisher (works for CRUD events)
        else if (options?.allAggregates && options.allAggregates.length > 0) {
            const found = this.findEventPublisher(eventTypeName, options.allAggregates);
            if (found) {
                eventSourceAggregate = found;
            } else {
                console.warn(`Warning: Could not find publisher aggregate for event ${eventTypeName}`);
                // Fallback to inferring from event name
                const inferredPublisher = eventTypeName.replace(/(Deleted|Updated|Created)?Event$/, '');
                eventSourceAggregate = inferredPublisher.toLowerCase();
            }
        }
        // PRIORITY 3: Fallback (only when allAggregates not available)
        else {
            // Infer the publishing aggregate from the event name
            // Extract entity name from event (e.g., UserDeletedEvent -> User, TopicUpdatedEvent -> Topic)
            // Pattern is: {Entity}{Action}Event where Action is Deleted, Updated, or Created
            const inferredPublisher = eventTypeName.replace(/(Deleted|Updated|Created)?Event$/, '');
            eventSourceAggregate = inferredPublisher.toLowerCase();
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

    private buildInterInvariantSubscriptionContext(event: SubscribedEvent, aggregate: Aggregate, options: any): any {
        const aggregateName = aggregate.name;
        const eventTypeName = (event as any).eventType || 'UnknownEvent';
        const eventBaseName = eventTypeName.replace(/Event$/, '');

        // For inter-invariants, include the field name in the subscription class name to avoid conflicts
        // when multiple inter-invariants subscribe to the same event
        const interInvariantName = (event as any).interInvariantName || '';
        // Convert TOURNAMENT_CREATOR_EXISTS to TournamentCreatorExists
        const fieldSuffix = interInvariantName
            ? interInvariantName.split('_').map((word: string) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join('')
            : '';
        const subscriptionName = `${aggregateName}Subscribes${eventBaseName}${fieldSuffix}`;

        // Extract entity reference from condition
        const entityRef = this.extractEntityReferenceFromCondition(event, aggregate);
        if (!entityRef) {
            console.warn(`Warning: Could not extract entity reference for inter-invariant subscription ${subscriptionName}`);
            return this.buildSubscribedEventContext(event, aggregate, options);
        }

        // Determine source aggregate from event
        // First check if it's explicitly specified in the DSL with "from Aggregate"
        let eventSourceAggregate = aggregateName.toLowerCase();
        if (event.sourceAggregate) {
            eventSourceAggregate = event.sourceAggregate.toLowerCase();
        } else {
            // Check if the referenced entity is a projection entity
            const entities = (aggregate as any).entities || [];
            const referencedEntity = entities.find((e: any) => e.name === entityRef.entityTypeName);

            if (referencedEntity && (referencedEntity as any).aggregateRef) {
                // It's a projection entity - use the aggregate it projects from
                eventSourceAggregate = (referencedEntity as any).aggregateRef.toLowerCase();
            } else {
                // Try AST reference
                const publishedEvent = (event as any).eventType?.ref;
                const eventsContainer = publishedEvent?.$container;
                const sourceAggregate = eventsContainer?.$container;
                if (sourceAggregate?.name) {
                    eventSourceAggregate = sourceAggregate.name.toLowerCase();
                } else {
                    // Infer from event name
                    const inferredPublisher = eventTypeName.replace(/(Deleted|Updated|Created)?Event$/, '');
                    eventSourceAggregate = inferredPublisher.toLowerCase();
                }
            }
        }

        // Build field names from entity type
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
                `import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${eventSourceAggregate}.events.publish.${eventTypeName};`,
                ''
            ]
        };
    }

    private extractEntityReferenceFromCondition(event: SubscribedEvent, aggregate: Aggregate): { fieldName: string, entityTypeName: string } | null {
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

        // Extract field name from "fieldName.property == ..."
        const match = conditionText.match(/^(\w+)\./);
        if (!match) {
            return null;
        }

        const fieldName = match[1];

        // Find the property in the root entity
        const rootEntity: any = (aggregate as any).entities?.find((e: any) => e.isRoot);
        if (!rootEntity) {
            return null;
        }

        const property = rootEntity.properties.find((p: any) => p.name === fieldName);
        if (!property) {
            return null;
        }

        // Extract entity type name
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

        // CollectionType pattern (Set<T>, List<T>)
        if (typeObj.$type === 'CollectionType' && typeObj.elementType) {
            const elementType = typeObj.elementType;
            if (elementType.$refText) {
                return elementType.$refText;
            }
            if (elementType.type?.$refText) {
                return elementType.type.$refText;
            }
        }

        // EntityType pattern
        if (typeObj.$type === 'EntityType' && typeObj.type?.$refText) {
            return typeObj.type.$refText;
        }

        // Simple entity type (legacy pattern)
        if (typeObj.type?.$refText) {
            return typeObj.type.$refText;
        }

        // Collection type (legacy pattern)
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

    private buildCustomEventHandlingContext(aggregate: Aggregate, options: any): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();

        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);

        // Filter out inter-invariant subscriptions as they don't have handlers
        const eventsWithHandlers = allSubscribedEvents.filter((event: any) => !(event as any).isInterInvariant);

        const subscribedEvents = eventsWithHandlers.map(event => {
            const eventTypeName = (event as any).eventType || 'UnknownEvent';
            const handlerName = `${eventTypeName}Handler`;
            return { eventName: eventTypeName, handlerName };
        }) || [];

        const subscribedEventImports = eventsWithHandlers.map(event => {
            const eventTypeName = (event as any).eventType || 'UnknownEvent';
            const handlerName = `${eventTypeName}Handler`;

            // Extract entity name from event (e.g., UserDeletedEvent -> User, TopicUpdatedEvent -> Topic)
            const entityName = eventTypeName.replace(/(Updated|Deleted|Created)Event$/, '');

            // Determine source aggregate from published event
            let sourceAggregateName = lowerAggregateName;

            // PRIORITY 1: Try AST reference (works for custom events with explicit references)
            const publishedEvent = (event as any).eventType?.ref;
            const eventsContainer = publishedEvent?.$container;
            const sourceAggregate = eventsContainer?.$container;
            if (sourceAggregate?.name) {
                sourceAggregateName = sourceAggregate.name.toLowerCase();
            } else if ((event as any).sourceAggregate) {
                sourceAggregateName = ((event as any).sourceAggregate as string).toLowerCase();
            }
            // PRIORITY 2: Search all aggregates for event publisher (works for CRUD events)
            else if (options?.allAggregates && options.allAggregates.length > 0) {
                const found = this.findEventPublisher(eventTypeName, options.allAggregates);
                if (found) {
                    sourceAggregateName = found;
                } else {
                    console.warn(`Warning: Could not find publisher aggregate for event ${eventTypeName}`);
                    // Fallback to simple name matching
                    sourceAggregateName = entityName.toLowerCase();
                }
            }
            // PRIORITY 3: Fallback (only when allAggregates not available)
            else {
                // Fallback: infer source aggregate from event name
                // UserDeletedEvent -> user, TopicUpdatedEvent -> topic
                sourceAggregateName = entityName.toLowerCase();
            }

            return {
                handlerName,
                handlerPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
                eventName: eventTypeName,
                eventPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${sourceAggregateName}.events.publish`
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

    private buildEventHandlerContext(event: SubscribedEvent, aggregate: Aggregate, options: any): any {
        const aggregateName = aggregate.name;
        const lowerAggregateName = aggregateName.toLowerCase();
        const eventTypeName = (event as any).eventType || 'UnknownEvent';
        const handlerName = `${eventTypeName}Handler`;

        // Extract entity name from event (e.g., UserDeletedEvent -> User, TopicUpdatedEvent -> Topic)
        const entityName = eventTypeName.replace(/(Updated|Deleted|Created)Event$/, '');

        // Determine source aggregate from published event
        let sourceAggregateName = lowerAggregateName;

        // PRIORITY 1: Try AST reference (works for custom events with explicit references)
        const publishedEvent = (event as any).eventType?.ref;
        const eventsContainer = publishedEvent?.$container;
        const sourceAggregate = eventsContainer?.$container;
        if (sourceAggregate?.name) {
            sourceAggregateName = sourceAggregate.name.toLowerCase();
        } else if ((event as any).sourceAggregate) {
            sourceAggregateName = ((event as any).sourceAggregate as string).toLowerCase();
        }
        // PRIORITY 2: Search all aggregates for event publisher (works for CRUD events)
        else if (options?.allAggregates && options.allAggregates.length > 0) {
            const found = this.findEventPublisher(eventTypeName, options.allAggregates);
            if (found) {
                sourceAggregateName = found;
            } else {
                console.warn(`Warning: Could not find publisher aggregate for event ${eventTypeName}`);
                // Fallback to simple name matching
                sourceAggregateName = entityName.toLowerCase();
            }
        }
        // PRIORITY 3: Fallback (only when allAggregates not available)
        else {
            // Fallback: infer source aggregate from event name
            // UserDeletedEvent -> user, TopicUpdatedEvent -> topic
            sourceAggregateName = entityName.toLowerCase();
        }

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.events.handling.handlers`,
            handlerName,
            aggregateName,
            lowerAggregateName,
            eventTypeName,
            aggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`,
            aggregateCoordinationPackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.coordination.eventProcessing`,
            eventTypePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${sourceAggregateName}.events.publish`
        };
    }

    private collectSubscribedEvents(aggregate: Aggregate): any[] {
        const direct = aggregate.events?.subscribedEvents || [];
        const interInvariants = (aggregate.events as any)?.interInvariants || [];
        const interSubs = interInvariants.flatMap((ii: any) =>
            (ii?.subscribedEvents || []).map((sub: any) => ({ ...sub, isInterInvariant: true }))
        );
        const allSubscribed = [...direct, ...interSubs];

        // Deduplicate by event type name to avoid duplicate handlers
        const eventMap = new Map<string, any>();
        allSubscribed.forEach((event: any) => {
            const eventTypeName = (event as any).eventType || 'UnknownEvent';
            if (!eventMap.has(eventTypeName)) {
                eventMap.set(eventTypeName, event);
            }
        });

        return Array.from(eventMap.values());
    }

    /**
     * Find which aggregate publishes a given event by checking all aggregates' published events.
     * Handles both custom published events and auto-generated CRUD events.
     */
    private findEventPublisher(eventTypeName: string, allAggregates: Aggregate[]): string | null {
        for (const agg of allAggregates) {
            const aggName = agg.name;

            // 1. Check custom published events (explicitly defined in DSL)
            const aggregateEvents = (agg as any).events;
            const customEvents = aggregateEvents?.publishedEvents || [];
            if (customEvents.some((e: any) => e.name === eventTypeName)) {
                return aggName.toLowerCase();
            }

            // 2. Check root entity CRUD events (auto-generated for @GenerateCrud)
            if (agg.generateCrud) {
                const rootCrudEvents = [
                    `${aggName}UpdatedEvent`,
                    `${aggName}DeletedEvent`
                ];
                if (rootCrudEvents.includes(eventTypeName)) {
                    return aggName.toLowerCase();
                }
            }

            // 3. Check projection entity CRUD events
            const projectionEntities = (agg.entities || []).filter((e: any) =>
                !e.isRoot && e.aggregateRef
            );
            for (const proj of projectionEntities) {
                const projName = proj.name;
                const projCrudEvents = [
                    `${projName}UpdatedEvent`,
                    `${projName}DeletedEvent`,
                    `${projName}RemovedEvent`  // For collection manipulation
                ];
                if (projCrudEvents.includes(eventTypeName)) {
                    return aggName.toLowerCase();  // Return AGGREGATE name, not projection name
                }
            }
        }

        return null;  // Not found in any aggregate
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