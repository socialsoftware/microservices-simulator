import { Aggregate, Entity } from '../../base/model-parser.js';
import { CoordinationGenerationOptions } from '../types.js';
import { OrchestrationBase } from '../../base/orchestration-base.js';

export class EventProcessingGenerator extends OrchestrationBase {
    async generate(aggregate: Aggregate, rootEntity: Entity, options: CoordinationGenerationOptions): Promise<string> {
        const context = this.buildContext(aggregate, rootEntity, options);
        const template = this.buildTemplateString();
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

        const renderedMethods = eventProcessingMethodsArray.map((method: any) => this.renderMethod(method, tempContext)).join('\n\n');

        return {
            ...tempContext,
            eventProcessingMethods: renderedMethods
        };
    }

    private buildEventProcessingMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string): any[] {
        const methods: any[] = [];

        methods.push({
            name: `process${aggregateName}Event`,
            returnType: 'void',
            parameters: [
                { type: 'String', name: 'eventType' },
                { type: 'Integer', name: 'aggregateId' },
                { type: 'Integer', name: 'aggregateVersion' }
            ]
        });

        const aggregateEvents = (aggregate as any).events;
        if (aggregateEvents) {
            // Handle new DSL structure with publishedEvents and subscribedEvents
            if (aggregateEvents.subscribedEvents) {
                aggregateEvents.subscribedEvents.forEach((event: any) => {
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
            }

            // Handle legacy array structure for backward compatibility
            if (Array.isArray(aggregateEvents)) {
                aggregateEvents.forEach((event: any) => {
                    methods.push({
                        name: `process${event.name}`,
                        returnType: 'void',
                        parameters: event.parameters || []
                    });
                });
            }
        }

        return methods;
    }

    private buildImports(aggregate: Aggregate, options: CoordinationGenerationOptions): string[] {
        const imports: string[] = [];
        const projectName = options.projectName.toLowerCase();

        const basePackage = this.getBasePackage();
        imports.push(`import static ${basePackage}.ms.TransactionalModel.${this.getTransactionModel()};`);
        imports.push(`import static ${basePackage}.${projectName}.microservices.exception.${this.capitalize(options.projectName)}ErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;`);

        imports.push('import java.util.Arrays;');
        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.core.env.Environment;');
        imports.push('import org.springframework.stereotype.Service;');
        imports.push('import jakarta.annotation.PostConstruct;');
        imports.push(`import ${basePackage}.ms.TransactionalModel;`);
        imports.push(`import ${basePackage}.ms.coordination.unitOfWork.UnitOfWork;`);
        imports.push(`import ${basePackage}.ms.coordination.unitOfWork.UnitOfWorkService;`);
        imports.push(`import ${basePackage}.${projectName}.microservices.exception.${this.capitalize(options.projectName)}Exception;`);
        imports.push(`import ${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.service.${this.capitalize(aggregate.name)}Service;`);

        // Add imports for subscribed events
        const aggregateEvents = (aggregate as any).events;
        if (aggregateEvents?.subscribedEvents) {
            aggregateEvents.subscribedEvents.forEach((event: any) => {
                const eventTypeName = event.eventType?.ref?.name || event.eventType?.$refText || 'UnknownEvent';

                // Determine the source aggregate for the event (e.g., DeleteUserEvent comes from User aggregate)
                let eventSourceAggregate = 'user'; // Default assumption for DeleteUserEvent
                if (eventTypeName.toLowerCase().includes('user')) {
                    eventSourceAggregate = 'user';
                } else if (eventTypeName.toLowerCase().includes('course')) {
                    eventSourceAggregate = 'course';
                } else if (eventTypeName.toLowerCase().includes('execution')) {
                    eventSourceAggregate = 'execution';
                }

                imports.push(`import ${basePackage}.${projectName}.microservices.${eventSourceAggregate}.events.publish.${eventTypeName};`);
            });
        }

        return imports;
    }

    private buildTemplateString(): string {
        return `package {{packageName}};

{{imports}}

@Service
public class {{aggregateName}}EventProcessing {
    @Autowired
    private {{aggregateName}}Service {{lowerAggregate}}Service;
    
    @Autowired
    private UnitOfWorkService unitOfWorkService;
    
    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        workflowType = Arrays.asList(activeProfiles).contains({{transactionModel}}.getValue()) ? {{transactionModel}} : null;
        if (workflowType == null) {
            throw new {{ProjectName}}Exception(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

{{eventProcessingMethods}}
}`;
    }

    private renderMethod(method: any, context: any): string {
        const params = method.parameters.map((p: any) => `${p.type} ${p.name}`).join(', ');

        return `    public ${method.returnType} ${method.name}(${params}) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        unitOfWorkService.commit(unitOfWork);
    }`;
    }

}
