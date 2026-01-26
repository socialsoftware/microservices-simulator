import { Aggregate, Entity } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { OrchestrationBase } from '../common/orchestration-base.js';

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
            const eventTypeName = event.eventType?.ref?.name || event.eventType?.$refText || 'UnknownEvent';
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
            const eventTypeName = event.eventType?.ref?.name || event.eventType?.$refText || 'UnknownEvent';
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

        // Derive service method name from event name
        const serviceMethodName = this.deriveServiceMethodName(eventTypeName);
        
        // Build service method call parameters
        const serviceCallParams = this.buildServiceMethodParams(eventTypeName, eventVarName, aggregateIdName);

        return `    public ${method.returnType} ${method.name}(${params}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        ${context.lowerAggregate}Service.${serviceMethodName}(${serviceCallParams});
        unitOfWorkService.commit(unitOfWork);
    }`;
    }

    private deriveServiceMethodName(eventTypeName: string): string {
        // Remove "Event" suffix
        const nameWithoutEvent = eventTypeName.replace(/Event$/, '');
        
        // Handle common patterns
        if (nameWithoutEvent.startsWith('Update')) {
            const entityName = nameWithoutEvent.replace(/^Update/, '');
            return `update${entityName}`;
        } else if (nameWithoutEvent.startsWith('Delete')) {
            const entityName = nameWithoutEvent.replace(/^Delete/, '');
            return `remove${entityName}`;
        } else if (nameWithoutEvent.startsWith('Remove')) {
            const entityName = nameWithoutEvent.replace(/^Remove/, '');
            return `remove${entityName}`;
        }
        
        // Default: convert to camelCase
        return nameWithoutEvent.charAt(0).toLowerCase() + nameWithoutEvent.slice(1);
    }

    private buildServiceMethodParams(eventTypeName: string, eventVarName: string, aggregateIdName: string): string {
        // Build common parameters: aggregateId, publisherAggregateId, publisherAggregateVersion, unitOfWork
        // This is a simplified version - actual implementation may need to inspect event fields
        const nameWithoutEvent = eventTypeName.replace(/Event$/, '');
        
        // For Update events, typically include fields from the event
        if (nameWithoutEvent.startsWith('Update')) {
            // Try to extract entity name and common fields
            if (nameWithoutEvent.includes('Topic')) {
                return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getTopicName(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
            } else if (nameWithoutEvent.includes('Question')) {
                return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getTitle(), ${eventVarName}.getContent(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
            } else if (nameWithoutEvent.includes('CourseExecution')) {
                return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
            }
            // Generic update pattern
            return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
        } else if (nameWithoutEvent.startsWith('Delete') || nameWithoutEvent.startsWith('Remove')) {
            // For Delete/Remove events, typically just aggregateId, publisherAggregateId, version, unitOfWork
            if (nameWithoutEvent.includes('CourseExecution')) {
                return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
            } else if (nameWithoutEvent.includes('Question')) {
                // Some remove methods don't take version
                if (nameWithoutEvent.includes('QuizQuestion')) {
                    return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), unitOfWork`;
                }
                return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
            }
            // Generic delete pattern
            return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
        }
        
        // Default fallback
        return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
    }


}
