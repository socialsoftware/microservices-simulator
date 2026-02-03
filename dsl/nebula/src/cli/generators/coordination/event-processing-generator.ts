import { Aggregate, Entity } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { OrchestrationBase } from '../common/orchestration-base.js';
import { getEntities } from '../../utils/aggregate-helpers.js';

export class EventProcessingGenerator extends OrchestrationBase {
    async generate(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions): Promise<string> {
        const context = this.buildContext(aggregate, rootEntity, options);
        // Check if there are methods before building template
        const hasMethods = context.eventProcessingMethods !== undefined && context.eventProcessingMethods.trim().length > 0;
        const template = this.buildTemplateString(context, hasMethods);
        return this.renderSimpleTemplate(template, context);
    }

    private buildContext(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const projectName = options.projectName.toLowerCase();
        const ProjectName = this.capitalize(options.projectName);

        const eventProcessingMethodsArray = this.buildEventProcessingMethods(aggregate, rootEntity, capitalizedAggregate);
        const imports = this.buildImports(aggregate, options);

        const basePackage = this.getBasePackage();
        const tempContext = {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${basePackage}.${projectName}.coordination.eventProcessing`,
            basePackage,
            transactionModel: this.getTransactionModel(),
            imports: imports.join('\n'),
            projectName,
            ProjectName,
            hasSagas: options.architecture === 'causal-saga' || options.features?.includes('sagas')
        };

        const renderedMethods = eventProcessingMethodsArray.map((method: any) => this.renderMethod(method, tempContext, aggregate)).join('\n\n');

        return {
            ...tempContext,
            eventProcessingMethods: renderedMethods.trim().length > 0 ? renderedMethods : undefined
        };
    }

    private buildEventProcessingMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string): any[] {
        const methods: any[] = [];

        // Collect all subscribed events (direct + interInvariants)
        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);

        allSubscribedEvents.forEach((event: any) => {
            const eventTypeName = event.eventType || 'UnknownEvent';
            methods.push({
                name: `process${eventTypeName}`,
                returnType: 'void',
                parameters: [
                    { name: 'aggregateId', type: 'Integer' },
                    { name: eventTypeName.charAt(0).toLowerCase() + eventTypeName.slice(1), type: eventTypeName }
                ]
            });
        });

        // Handle legacy array structure for backward compatibility
        const aggregateEvents = (aggregate as any).events;
        if (aggregateEvents && Array.isArray(aggregateEvents)) {
            aggregateEvents.forEach((event: any) => {
                methods.push({
                    name: `process${event.name}`,
                    returnType: 'void',
                    parameters: event.parameters || []
                });
            });
        }

        return methods;
    }

    /**
     * Collect all subscribed events from direct subscriptions and interInvariants
     * Deduplicates events by event type name to avoid duplicate methods
     */
    private collectSubscribedEvents(aggregate: Aggregate): any[] {
        const aggregateEvents = (aggregate as any).events;
        if (!aggregateEvents) {
            return [];
        }

        const directSubscribed = aggregateEvents.subscribedEvents || [];
        const interSubscribed = aggregateEvents.interInvariants?.flatMap((ii: any) => ii?.subscribedEvents || []) || [];
        const allSubscribed = [...directSubscribed, ...interSubscribed];

        // Deduplicate by event type name
        const eventMap = new Map<string, any>();
        allSubscribed.forEach((event: any) => {
            const eventTypeName = event.eventType || 'UnknownEvent';
            if (!eventMap.has(eventTypeName)) {
                eventMap.set(eventTypeName, event);
            }
        });

        return Array.from(eventMap.values());
    }

    private buildImports(aggregate: Aggregate, options: CoordinationGenerationOptions): string[] {
        const imports: string[] = [];
        const projectName = options.projectName.toLowerCase();
        const basePackage = this.getBasePackage();

        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.stereotype.Service;');
        imports.push(`import ${basePackage}.ms.coordination.unitOfWork.UnitOfWork;`);
        imports.push(`import ${basePackage}.ms.coordination.unitOfWork.UnitOfWorkService;`);
        imports.push(`import ${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.service.${this.capitalize(aggregate.name)}Service;`);

        // Add imports for subscribed events (direct + interInvariants)
        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);

        allSubscribedEvents.forEach((event: any) => {
            const eventTypeName = event.eventType?.ref?.name || event.eventType?.$refText || 'UnknownEvent';

            // Determine the actual source aggregate (the aggregate that declares the published event)
            let sourceAggregateName = 'unknown';
            const publishedEvent = event.eventType?.ref as any;
            const eventsContainer = publishedEvent?.$container as any;
            const sourceAggregate = eventsContainer?.$container as Aggregate | undefined;
            if (sourceAggregate?.name) {
                sourceAggregateName = sourceAggregate.name.toLowerCase();
            } else if (event.sourceAggregate) {
                sourceAggregateName = event.sourceAggregate.toLowerCase();
            }

            imports.push(`import ${basePackage}.${projectName}.microservices.${sourceAggregateName}.events.publish.${eventTypeName};`);
        });

        return imports;
    }

    private buildTemplateString(context: any, hasMethods: boolean): string {
        // Only include methods section if there are methods
        const methodsSection = hasMethods ? '\n\n{{eventProcessingMethods}}\n' : '';
        
        return `package {{packageName}};

{{imports}}

@Service
public class {{aggregateName}}EventProcessing {
    @Autowired
    private {{aggregateName}}Service {{lowerAggregate}}Service;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public {{aggregateName}}EventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }${methodsSection}}`;
    }

    private renderMethod(method: any, context: any, aggregate: Aggregate): string {
        const params = method.parameters.map((p: any) => `${p.type} ${p.name}`).join(', ');

        // For event processing methods, generate simple UnitOfWork-based logic
        const eventParam = method.parameters.find((p: any) => p.type !== 'Integer' || p.name !== 'aggregateId');
        const eventTypeName = eventParam ? eventParam.type : 'UnknownEvent';
        const eventVarName = eventParam ? eventParam.name : 'event';
        const aggregateIdParam = method.parameters.find((p: any) => p.name === 'aggregateId');
        const aggregateIdName = aggregateIdParam ? aggregateIdParam.name : 'aggregateId';

        // Find the subscribed event to pass to buildServiceMethodParams
        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);
        const subscribedEvent = allSubscribedEvents.find((e: any) => {
            const eTypeName = e.eventType || 'UnknownEvent';
            return eTypeName === eventTypeName;
        });
        
        // Derive service method name from event name
        const serviceMethodName = this.deriveServiceMethodName(eventTypeName, aggregate);
        
        // Build service method call parameters
        const serviceCallParams = this.buildServiceMethodParams(eventTypeName, eventVarName, aggregateIdName, aggregate, subscribedEvent || {});

        return `    public ${method.returnType} ${method.name}(${params}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        ${context.lowerAggregate}Service.${serviceMethodName}(${serviceCallParams});
        unitOfWorkService.commit(unitOfWork);
    }`;
    }

    private deriveServiceMethodName(eventTypeName: string, aggregate: Aggregate): string {
        // Remove "Event" suffix
        const nameWithoutEvent = eventTypeName.replace(/Event$/, '');
        
        // Extract publisher aggregate name (e.g., UpdateTopicEvent -> Topic)
        const publisherAggregateName = nameWithoutEvent.replace(/^(Update|Delete|Remove|Create)/, '');
        
        // Find entities in this aggregate that use the publisher aggregate
        const entities = getEntities(aggregate);
        const projectionEntities = entities.filter((e: any) => {
            const aggregateRef = e.aggregateRef;
            return aggregateRef && aggregateRef.toLowerCase() === publisherAggregateName.toLowerCase();
        });
        
        // Handle common patterns
        if (nameWithoutEvent.startsWith('Update')) {
            if (projectionEntities.length > 0) {
                // Use the first projection entity name (e.g., QuestionTopic -> updateTopic)
                const projectionEntityName = projectionEntities[0].name;
                // Extract the part after the aggregate name (e.g., QuestionTopic -> Topic)
                const projectionPart = projectionEntityName.replace(new RegExp(`^${aggregate.name}`, 'i'), '');
                return `update${projectionPart}`;
            }
            const entityName = nameWithoutEvent.replace(/^Update/, '');
            return `update${entityName}`;
        } else if (nameWithoutEvent.startsWith('Delete')) {
            if (projectionEntities.length > 0) {
                const projectionEntityName = projectionEntities[0].name;
                const projectionPart = projectionEntityName.replace(new RegExp(`^${aggregate.name}`, 'i'), '');
                return `remove${projectionPart}`;
            }
            const entityName = nameWithoutEvent.replace(/^Delete/, '');
            return `remove${entityName}`;
        } else if (nameWithoutEvent.startsWith('Remove')) {
            if (projectionEntities.length > 0) {
                const projectionEntityName = projectionEntities[0].name;
                const projectionPart = projectionEntityName.replace(new RegExp(`^${aggregate.name}`, 'i'), '');
                return `remove${projectionPart}`;
            }
            const entityName = nameWithoutEvent.replace(/^Remove/, '');
            return `remove${entityName}`;
        }
        
        // Default: convert to camelCase
        return nameWithoutEvent.charAt(0).toLowerCase() + nameWithoutEvent.slice(1);
    }

    private buildServiceMethodParams(eventTypeName: string, eventVarName: string, aggregateIdName: string, aggregate: Aggregate, subscribedEvent: any): string {
        const nameWithoutEvent = eventTypeName.replace(/Event$/, '');
        const isUpdate = nameWithoutEvent.startsWith('Update');
        const isDelete = nameWithoutEvent.startsWith('Delete') || nameWithoutEvent.startsWith('Remove');
        
        // Extract publisher aggregate name from event (e.g., UpdateTopicEvent -> Topic)
        const publisherAggregateName = nameWithoutEvent.replace(/^(Update|Delete|Remove|Create)/, '');
        
        // Find entities in this aggregate that use the publisher aggregate
        const entities = getEntities(aggregate);
        const projectionEntities = entities.filter((e: any) => {
            const aggregateRef = e.aggregateRef;
            return aggregateRef && aggregateRef.toLowerCase() === publisherAggregateName.toLowerCase();
        });
        
        if (projectionEntities.length === 0) {
            // No projection found, use generic pattern
            if (isUpdate) {
                return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
            } else if (isDelete) {
                return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
            }
            return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
        }
        
        // Get event fields from the published event
        let eventFields: any[] = [];
        if (subscribedEvent && subscribedEvent.eventType) {
            const publishedEvent = subscribedEvent.eventType.ref as any;
            eventFields = publishedEvent?.fields || [];
        }
        
        // For Update events, extract field values from event
        if (isUpdate) {
            const fieldParams: string[] = [];
            // Common fields: publisherAggregateId, then event-specific fields, then version
            fieldParams.push(`${eventVarName}.getPublisherAggregateId()`);
            
            // Add event-specific fields (skip aggregateId, version, state as those are handled separately)
            for (const field of eventFields) {
                const fieldName = field.name;
                if (fieldName !== 'aggregateId' && fieldName !== 'version' && fieldName !== 'state') {
                    const capitalizedField = fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
                    fieldParams.push(`${eventVarName}.get${capitalizedField}()`);
                }
            }
            
            fieldParams.push(`${eventVarName}.getPublisherAggregateVersion()`);
            fieldParams.push('unitOfWork');
            
            return `${aggregateIdName}, ${fieldParams.join(', ')}`;
        } else if (isDelete) {
            // For Delete events: aggregateId, publisherAggregateId, version, unitOfWork
            return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
        }
        
        // Default fallback
        return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
    }


}
