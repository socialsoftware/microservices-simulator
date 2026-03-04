import { Aggregate } from "../../../../../language/generated/ast.js";
import { EntityExt, AggregateExt, TypeGuards } from "../../../../types/ast-extensions.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { getEvents, getEntities } from "../../../../utils/aggregate-helpers.js";
import { StringUtils } from '../../../../utils/string-utils.js';



interface EntityReference {
    fieldName: string;
    fieldType: string;
    isCollection: boolean;
}

interface EntitySubscriptionGroup {
    fieldName: string;
    fieldType: string;
    isCollection: boolean;
    subscriptions: SubscriptionInfo[];
}

interface SubscriptionInfo {
    subscriptionClass: string;
    eventType: string;
}



export class InterInvariantBuilder {
    

    generateInterInvariantMethods(aggregate: AggregateExt | undefined): string {
        if (!aggregate) return '';

        const events = getEvents(aggregate);
        const interInvariants = (events as any)?.interInvariants || [];
        if (interInvariants.length === 0) return '';

        const entities = getEntities(aggregate);
        const rootEntity = entities.find(e => TypeGuards.isRootEntity(e as EntityExt));
        if (!rootEntity) return '';

        return interInvariants.map((invariant: any) =>
            this.generateInterInvariantMethod(invariant, aggregate, rootEntity)
        ).join('\n\n');
    }

    

    private generateInterInvariantMethod(invariant: any, aggregate: AggregateExt, rootEntity: EntityExt): string {
        const methodName = `interInvariant${this.toCamelCase(invariant.name)}`;
        const subscribedEvents = invariant.subscribedEvents || [];

        
        const groupedSubs = this.groupSubscriptionsByEntity(subscribedEvents, rootEntity, invariant.name);

        let methodBody = `    private void ${methodName}(Set<EventSubscription> eventSubscriptions) {`;

        for (const group of groupedSubs) {
            if (group.isCollection) {
                
                const elementType = this.extractElementType(group.fieldType);
                methodBody += `\n        for (${elementType} item : this.${group.fieldName}) {`;
                for (const sub of group.subscriptions) {
                    methodBody += `\n            eventSubscriptions.add(new ${sub.subscriptionClass}(item));`;
                }
                methodBody += `\n        }`;
            } else {
                
                for (const sub of group.subscriptions) {
                    methodBody += `\n        eventSubscriptions.add(new ${sub.subscriptionClass}(this.get${StringUtils.capitalize(group.fieldName)}()));`;
                }
            }
        }

        methodBody += `\n    }`;
        return methodBody;
    }

    

    private groupSubscriptionsByEntity(subscriptions: any[], rootEntity: EntityExt, invariantName: string): EntitySubscriptionGroup[] {
        const groups = new Map<string, EntitySubscriptionGroup>();

        for (const sub of subscriptions) {
            const entityRef = this.extractEntityReference(sub, rootEntity);
            if (!entityRef) {
                continue;
            }

            if (!groups.has(entityRef.fieldName)) {
                groups.set(entityRef.fieldName, {
                    fieldName: entityRef.fieldName,
                    fieldType: entityRef.fieldType,
                    isCollection: entityRef.isCollection,
                    subscriptions: []
                });
            }

            const subscriptionClass = this.buildSubscriptionClassName(sub, rootEntity, invariantName);
            groups.get(entityRef.fieldName)!.subscriptions.push({
                subscriptionClass,
                eventType: this.extractEventTypeName(sub)
            });
        }

        return Array.from(groups.values());
    }

    

    private extractEntityReference(subscription: any, rootEntity: EntityExt): EntityReference | null {
        
        
        const conditions = subscription.conditions || [];
        if (conditions.length === 0) return null;

        const condition = conditions[0];

        
        let conditionText = '';
        if (condition.condition?.$cstNode?.text) {
            conditionText = condition.condition.$cstNode.text.trim();
        } else if (typeof condition === 'string') {
            conditionText = condition;
        } else if (typeof condition.condition === 'string') {
            conditionText = condition.condition;
        } else {
            return null;
        }

        
        const match = conditionText.match(/^(\w+)\./);
        if (!match) {
            return null;
        }

        const fieldName = match[1];

        
        const property = rootEntity.properties.find(p => p.name === fieldName);
        if (!property) {
            return null;
        }

        const javaType = TypeResolver.resolveJavaType(property.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

        return {
            fieldName,
            fieldType: javaType,
            isCollection
        };
    }

    

    private extractEventTypeName(subscription: any): string {
        if (typeof subscription.eventType === 'string') {
            return subscription.eventType;
        } else if (subscription.eventType?.ref?.name) {
            return subscription.eventType.ref.name;
        } else if (subscription.eventType?.$refText) {
            return subscription.eventType.$refText;
        }
        return 'UnknownEvent';
    }

    

    private buildSubscriptionClassName(subscription: any, rootEntity: EntityExt, invariantName: string): string {
        const eventTypeName = this.extractEventTypeName(subscription);
        
        const baseEventName = eventTypeName
            .replace(/Event$/, '')
            .replace(/^(Update|Delete|Create|Disenroll|Anonymize|Invalidate|Answer)/, '');

        const aggregate = rootEntity.$container as Aggregate;

        
        const interInvariantSuffix = invariantName
            .split('_')
            .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join('');

        return `${aggregate.name}Subscribes${baseEventName}${interInvariantSuffix}`;
    }

    

    private extractElementType(javaType: string): string {
        const match = javaType.match(/<(.+)>/);
        return match ? match[1] : 'Object';
    }

    

    private toCamelCase(snakeCaseUpper: string): string {
        return snakeCaseUpper
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join('');
    }

}
