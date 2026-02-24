import chalk from 'chalk';
import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, EventSubscriptionContext } from "./event-types.js";
import { getEntities } from "../../../utils/aggregate-helpers.js";
import { EventNameParser } from "../../common/utils/event-name-parser.js";

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

        
        const interInvariantContext = this.buildInterInvariantSubscriptionsContext(aggregate, rootEntity, options);
        for (const subscription of interInvariantContext.eventSubscriptions) {
            const subscriptionContext = {
                ...interInvariantContext,
                subscription
            };
            results[`event-subscription-${subscription.eventType}-${subscription.entityParamName}`] = await this.generateInterInvariantEventSubscription(subscriptionContext);
        }

        return results;
    }

    private buildEventSubscriptionsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventSubscriptionContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventSubscriptions = this.buildEventSubscriptions(aggregate, rootEntity, baseContext.aggregateName, options);
        const imports = this.buildEventSubscriptionsImports(aggregate, options, eventSubscriptions);

        return {
            ...baseContext,
            eventSubscriptions,
            imports
        };
    }

    private buildEventSubscriptions(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, options: EventGenerationOptions): any[] {
        
        const events = (aggregate as any).events;
        if (!events || !events.subscribedEvents) {
            return [];
        }

        
        const simpleSubscriptions = events.subscribedEvents.filter((sub: any) => {
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        const projectName = (this as any).projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getEventBasePackage(options);

        return simpleSubscriptions.map((sub: any) => {
            const eventTypeName = sub.eventType || 'UnknownEvent';


            let sourceAggregateName = 'unknown';

            if (sub.sourceAggregate) {
                sourceAggregateName = sub.sourceAggregate.toLowerCase();
            }

            else if (options?.allAggregates && options.allAggregates.length > 0) {
                const found = this.findEventPublisher(eventTypeName, options.allAggregates);
                if (found) {
                    sourceAggregateName = found;
                } else {
                    console.warn(chalk.yellow(`[WARN] Could not find publisher aggregate for event ${eventTypeName}`));

                    const entityName = EventNameParser.extractEntityName(eventTypeName);
                    sourceAggregateName = entityName.toLowerCase();
                }
            }

            else {
                console.warn(chalk.yellow(`[WARN] allAggregates not available for event ${eventTypeName}`));
                const entityName = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
                sourceAggregateName = entityName.toLowerCase();
            }


            const entityName = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
            const subscriptionClassName = `${aggregateName}Subscribes${entityName}`;


            const entities = getEntities(aggregate);
            const projectionEntities = entities.filter((e: any) => {
                const aggregateRef = e.aggregateRef;
                return aggregateRef && aggregateRef.toLowerCase() === entityName.toLowerCase();
            });
            
            const projectionEntity = projectionEntities.length > 0 ? projectionEntities[0] : null;
            const projectionEntityName = projectionEntity ? projectionEntity.name : null;
            const projectionPart = projectionEntityName ? projectionEntityName.replace(new RegExp(`^${aggregateName}`, 'i'), '') : entityName;
            
            
            const aggregateIdField = `${projectionPart.charAt(0).toLowerCase() + projectionPart.slice(1)}AggregateId`;
            const versionField = `${projectionPart.charAt(0).toLowerCase() + projectionPart.slice(1)}Version`;
            const capitalizedAggregateIdField = aggregateIdField.charAt(0).toUpperCase() + aggregateIdField.slice(1);
            const capitalizedVersionField = versionField.charAt(0).toUpperCase() + versionField.slice(1);
            
            return {
                eventType: EventNameParser.removeEventSuffix(eventTypeName),
                eventTypeName: eventTypeName,
                capitalizedEventName: eventTypeName,
                fullEventName: eventTypeName,
                capitalizedSubscriptionName: subscriptionClassName,
                subscriptionName: subscriptionClassName,
                sourceAggregate: sourceAggregateName,
                targetAggregate: aggregateName.toLowerCase(),
                projectionEntityName: projectionEntityName,
                projectionPart: projectionPart.charAt(0).toLowerCase() + projectionPart.slice(1),
                aggregateIdField: aggregateIdField,
                versionField: versionField,
                capitalizedAggregateIdField: capitalizedAggregateIdField,
                capitalizedVersionField: capitalizedVersionField,
                eventTypePackage: `${basePackage}.${projectName}.microservices.${sourceAggregateName}.events.publish`,
                isAsync: true,
                transactionPhase: 'AFTER_COMMIT'
            };
        });
    }

    private buildEventSubscriptionsImports(aggregate: Aggregate, options: EventGenerationOptions, eventSubscriptions: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const basePackage = this.getEventBasePackage(options);

        const imports = [
            ...baseImports,
            `import ${basePackage}.ms.domain.event.Event;`,
            `import ${basePackage}.ms.domain.event.EventSubscription;`
        ];

        imports.push('');
        return imports;
    }

    private async generateIndividualEventSubscription(context: any): Promise<string> {
        const template = this.loadRawTemplate('events/event-subscription.hbs');
        return this.renderTemplateFromString(template, context);
    }

    private buildInterInvariantSubscriptionsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): EventSubscriptionContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const eventSubscriptions = this.buildInterInvariantSubscriptions(aggregate, rootEntity, baseContext.aggregateName, options);
        const imports = this.buildInterInvariantSubscriptionsImports(aggregate, options, eventSubscriptions);

        return {
            ...baseContext,
            eventSubscriptions,
            imports
        };
    }

    private buildInterInvariantSubscriptions(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, options: EventGenerationOptions): any[] {
        const events = (aggregate as any).events;
        if (!events || !events.interInvariants) {
            console.log(`DEBUG: No inter-invariants found for ${aggregateName}`);
            return [];
        }

        console.log(`DEBUG: Found ${events.interInvariants.length} inter-invariants for ${aggregateName}`);

        const projectName = (this as any).projectName?.toLowerCase() || 'unknown';
        const basePackage = this.getEventBasePackage(options);
        const entities = getEntities(aggregate);

        const subscriptions: any[] = [];

        for (const interInvariant of events.interInvariants) {
            const subscribedEvents = interInvariant.subscribedEvents || [];
            console.log(`DEBUG: Processing inter-invariant ${interInvariant.name} with ${subscribedEvents.length} subscriptions`);

            for (const sub of subscribedEvents) {
                const eventTypeName = sub.eventType || 'UnknownEvent';
                const sourceAggregateName = sub.sourceAggregate?.toLowerCase() || this.extractSourceAggregateFromEvent(eventTypeName);

                
                const entityRef = this.extractEntityReferenceFromSubscription(sub, rootEntity);
                if (!entityRef) continue;

                
                const property = rootEntity.properties.find((p: any) => p.name === entityRef.fieldName);
                if (!property) continue;

                const entityTypeName = this.extractEntityType(property, entities);
                if (!entityTypeName) continue;

                const entityParamName = entityRef.fieldName;

                
                const eventBaseName = EventNameParser.removeEventSuffix(eventTypeName);
                const subscriptionClassName = `${aggregateName}Subscribes${eventBaseName}`;

                
                const { aggregateIdField, versionField } = this.buildFieldNamesFromEntity(entityTypeName, aggregateName);

                console.log(`DEBUG: Built subscription for ${subscriptionClassName}: entityType=${entityTypeName}, param=${entityParamName}, aggIdField=${aggregateIdField}`);

                subscriptions.push({
                    eventType: eventBaseName,
                    eventTypeName: eventTypeName,
                    capitalizedSubscriptionName: subscriptionClassName,
                    sourceAggregate: sourceAggregateName,
                    targetAggregate: aggregateName.toLowerCase(),
                    entityTypeName: entityTypeName,
                    entityParamName: entityParamName,
                    aggregateIdField: aggregateIdField,
                    versionField: versionField,
                    capitalizedAggregateIdField: aggregateIdField.charAt(0).toUpperCase() + aggregateIdField.slice(1),
                    capitalizedVersionField: versionField.charAt(0).toUpperCase() + versionField.slice(1),
                    eventTypePackage: `${basePackage}.${projectName}.microservices.${sourceAggregateName}.events.publish`,
                    entityTypePackage: `${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.aggregate`
                });
            }
        }

        return subscriptions;
    }

    private extractEntityReferenceFromSubscription(subscription: any, rootEntity: Entity): { fieldName: string } | null {
        const conditions = subscription.conditions || [];
        if (conditions.length === 0) return null;

        const condition = conditions[0];
        let conditionText = '';

        if (condition.condition?.$cstNode?.text) {
            conditionText = condition.condition.$cstNode.text.trim();
        } else {
            return null;
        }

        
        const match = conditionText.match(/^(\w+)\./);
        if (!match) return null;

        return { fieldName: match[1] };
    }

    private extractEntityType(property: any, entities: Entity[]): string | null {
        const typeObj = property.type;
        if (!typeObj) return null;

        
        
        if (typeObj.$type === 'EntityType') {
            return typeObj.type?.ref?.name || typeObj.type?.$refText || null;
        }

        
        if (typeObj.$type === 'ListType' || typeObj.$type === 'SetType') {
            const elementType = typeObj.elementType;
            if (elementType?.$type === 'EntityType') {
                return elementType.type?.ref?.name || elementType.type?.$refText || null;
            }
        }

        
        if (typeObj.$type === 'OptionalType') {
            const elementType = typeObj.elementType;
            if (elementType?.$type === 'EntityType') {
                return elementType.type?.ref?.name || elementType.type?.$refText || null;
            }
        }

        return null;
    }

    private buildFieldNamesFromEntity(entityTypeName: string, aggregateName: string): { aggregateIdField: string, versionField: string } {
        
        const cleanEntityName = entityTypeName.replace(new RegExp(`^${aggregateName}`, 'i'), '');
        const lowerEntityName = cleanEntityName.charAt(0).toLowerCase() + cleanEntityName.slice(1);

        return {
            aggregateIdField: `${lowerEntityName}AggregateId`,
            versionField: `${lowerEntityName}Version`
        };
    }

    private extractSourceAggregateFromEvent(eventTypeName: string): string {
        
        const baseName = EventNameParser.extractEntityName(eventTypeName);
        return baseName.toLowerCase();
    }

    private buildInterInvariantSubscriptionsImports(aggregate: Aggregate, options: EventGenerationOptions, eventSubscriptions: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);
        const basePackage = this.getEventBasePackage(options);

        const imports = [
            ...baseImports,
            `import ${basePackage}.ms.domain.event.Event;`,
            `import ${basePackage}.ms.domain.event.EventSubscription;`
        ];

        
        eventSubscriptions.forEach((sub: any) => {
            imports.push(`import ${sub.entityTypePackage}.${sub.entityTypeName};`);
            imports.push(`import ${sub.eventTypePackage}.${sub.eventTypeName};`);
        });

        imports.push('');
        return imports;
    }

    private async generateInterInvariantEventSubscription(context: any): Promise<string> {
        const template = this.loadRawTemplate('events/inter-invariant-subscription.hbs');
        return this.renderTemplateFromString(template, context);
    }
}
