import { Aggregate, PublishedEvent, SubscribedEvent, EventField } from "../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../shared/orchestration-base.js";
import { TypeResolver } from "../../shared/resolvers/type-resolver.js";

export class EventGenerator extends OrchestrationBase {

    // Method expected by existing EventsFeature
    async generateEvents(aggregate: Aggregate, options: any): Promise<any> {
        const result: any = {};

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            return result;
        }

        try {
            // Generate main event handling class (only for subscribed events)
            if (aggregate.events?.subscribedEvents && aggregate.events.subscribedEvents.length > 0) {
                const eventHandlingCode = this.generateCustomEventHandling(aggregate, options);
                result['event-handling'] = eventHandlingCode;
            }

            // Note: We only generate handlers for subscribed events, not standard CRUD handlers
            // Standard CRUD handlers are not needed for our DSL-based event system

            // Note: Standard CRUD published events (ExecutionCreatedEvent, etc.) are generated elsewhere
            // We only handle custom DSL-defined events in our custom event generation

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

        // Process event fields
        const fields = event.fields.map((field: EventField) => ({
            type: TypeResolver.resolveJavaType(field.type),
            name: field.name,
            capitalizedName: this.capitalize(field.name)
        }));

        // Generate imports for field types
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

        // The sourceAggregate in DSL now refers to the entity that creates the subscription
        // e.g., "from ExecutionStudent" means ExecutionStudent creates the subscription
        const subscribingEntityName = event.sourceAggregate; // e.g., "ExecutionStudent"
        const subscribingEntityVariable = subscribingEntityName.toLowerCase(); // e.g., "executionstudent"

        // The actual event comes from the User aggregate (based on event type)
        const eventSourceAggregate = 'user'; // DeleteUserEvent comes from User aggregate

        // Process conditions
        const conditions = event.conditions?.map((condition: any) => ({
            condition: condition.condition
        })) || [];

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName}.events.subscribe`,
            subscriptionName,
            sourceAggregate: subscribingEntityName,
            sourceAggregateVariable: subscribingEntityVariable,
            sourceAggregatePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName}.aggregate`,
            eventTypeName,
            eventTypePackage: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${eventSourceAggregate}.events.publish`,
            conditions
        };
    }

    private generatePublishedEventImports(fields: any[]): string {
        const imports = new Set<string>();

        // Check for specific types that need imports
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

        // Process subscribed events
        const subscribedEvents = aggregate.events?.subscribedEvents?.map(event => {
            const eventTypeName = event.eventType.ref?.name || event.eventType.$refText || 'UnknownEvent';
            const handlerName = `${eventTypeName}Handler`;

            return {
                eventName: eventTypeName,
                handlerName: handlerName
            };
        }) || [];

        // Build imports for subscribed events
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
}