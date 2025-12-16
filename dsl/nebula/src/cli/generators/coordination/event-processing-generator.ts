import { Aggregate, Entity } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { OrchestrationBase } from '../common/orchestration-base.js';

export class EventProcessingGenerator extends OrchestrationBase {
    async generate(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions): Promise<string> {
        const context = this.buildContext(aggregate, rootEntity, options);
        const template = this.buildTemplateString(context);
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
            eventProcessingMethods: renderedMethods
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
        const hasSagas = options.architecture === 'causal-saga' || options.features?.includes('sagas');

        const basePackage = this.getBasePackage();
        imports.push(`import static ${basePackage}.ms.TransactionalModel.SAGAS;`);
        imports.push(`import static ${basePackage}.${projectName}.microservices.exception.${this.capitalize(options.projectName)}ErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;`);

        imports.push('import java.util.Arrays;');
        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.core.env.Environment;');
        imports.push('import org.springframework.stereotype.Service;');
        imports.push('import jakarta.annotation.PostConstruct;');
        imports.push(`import ${basePackage}.ms.TransactionalModel;`);
        imports.push(`import ${basePackage}.${projectName}.microservices.exception.${this.capitalize(options.projectName)}Exception;`);
        imports.push(`import ${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.service.${this.capitalize(aggregate.name)}Service;`);

        // Add saga imports
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);

        // Add imports for subscribed events (direct + interInvariants)
        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();

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

            // Add saga class import if saga workflow exists for this event
            if (hasSagas) {
                const sagaWorkflow = this.findSagaWorkflowForEvent(aggregate, eventTypeName);
                if (sagaWorkflow) {
                    const sagaClassName = this.getSagaClassName(aggregateName, eventTypeName);
                    imports.push(`import ${basePackage}.${projectName}.sagas.coordination.${lowerAggregate}.${sagaClassName};`);
                }
            }
        });

        return imports;
    }

    private buildTemplateString(context: any): string {
        return `package {{packageName}};

{{imports}}

@Service
public class {{aggregateName}}EventProcessing {
    @Autowired
    private {{aggregateName}}Service {{lowerAggregate}}Service;
    
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        workflowType = Arrays.asList(activeProfiles).contains(SAGAS.getValue()) ? SAGAS : null;
        if (workflowType == null) {
            throw new {{ProjectName}}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

{{eventProcessingMethods}}
}`;
    }

    private renderMethod(method: any, context: any, aggregate: Aggregate): string {
        const params = method.parameters.map((p: any) => `${p.type} ${p.name}`).join(', ');

        // For event processing methods, generate saga workflow logic
        const eventParam = method.parameters.find((p: any) => p.type !== 'Integer' || p.name !== 'aggregateId');
        const eventTypeName = eventParam ? eventParam.type : 'UnknownEvent';
        const eventVarName = eventParam ? eventParam.name : 'event';
        const aggregateIdParam = method.parameters.find((p: any) => p.name === 'aggregateId');
        const aggregateIdName = aggregateIdParam ? aggregateIdParam.name : 'aggregateId';

        // Check if there's a saga workflow for this event
        const sagaWorkflow = this.findSagaWorkflowForEvent(aggregate, eventTypeName);
        const hasSagaWorkflow = sagaWorkflow !== null;

        if (hasSagaWorkflow) {
            // Generate saga workflow call
            const sagaClassName = this.getSagaClassName(context.aggregateName, eventTypeName);

            return `    public ${method.returnType} ${method.name}(${params}) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

        ${sagaClassName} ${this.camelCase(sagaClassName)} =
                new ${sagaClassName}(${context.lowerAggregate}Service, sagaUnitOfWorkService, ${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${this.buildSagaConstructorParams(eventTypeName, eventVarName, aggregateIdName)});
                
        ${this.camelCase(sagaClassName)}.executeWorkflow(sagaUnitOfWork);
    }`;
        } else {
            // Generate placeholder for events without saga workflows
            return `    public ${method.returnType} ${method.name}(${params}) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
        // TODO: Implement saga workflow for ${eventTypeName}
    }`;
        }
    }

    private findSagaWorkflowForEvent(aggregate: Aggregate, eventTypeName: string): any {
        const workflows = (aggregate as any).workflows || [];
        // Try to find a workflow that matches the event name pattern
        // e.g., "AnonymizeStudentEvent" -> "anonymizeStudent" or "anonymizeUser"
        const eventNameWithoutEvent = eventTypeName.replace(/Event$/, '');
        const lowerEventName = eventNameWithoutEvent.charAt(0).toLowerCase() + eventNameWithoutEvent.slice(1);

        return workflows.find((w: any) => {
            const workflowName = (w.name || '').toLowerCase();
            return workflowName === lowerEventName ||
                workflowName === lowerEventName.replace('student', 'user') ||
                workflowName === lowerEventName.replace('user', 'student');
        });
    }

    private getSagaClassName(aggregateName: string, eventTypeName: string): string {
        // Convert event name to saga class name
        // e.g., "AnonymizeStudentEvent" -> "AnonymizeUserTournamentFunctionalitySagas"
        // This is a simplified version - actual mapping may be more complex
        const eventNameWithoutEvent = eventTypeName.replace(/Event$/, '');
        const capitalized = eventNameWithoutEvent.charAt(0).toUpperCase() + eventNameWithoutEvent.slice(1);
        return `${capitalized}${aggregateName}FunctionalitySagas`;
    }

    private buildSagaConstructorParams(eventTypeName: string, eventVarName: string, aggregateIdName: string): string {
        // Build constructor parameters based on common event patterns
        // This is a simplified version - actual implementation may need to inspect event fields
        if (eventTypeName.includes('Anonymize')) {
            return `${eventVarName}.getStudentAggregateId(), ${eventVarName}.getName(), ${eventVarName}.getUsername(), ${eventVarName}.getPublisherAggregateVersion(), sagaUnitOfWork`;
        } else if (eventTypeName.includes('UpdateStudentName') || eventTypeName.includes('UpdateUserName')) {
            return `${eventVarName}.getPublisherAggregateVersion(), ${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getStudentAggregateId(), sagaUnitOfWork, ${eventVarName}.getUpdatedName()`;
        } else {
            // Generic fallback
            return `${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), sagaUnitOfWork`;
        }
    }

    private camelCase(str: string): string {
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

}
