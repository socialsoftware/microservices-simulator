import { AggregateExt, EntityExt } from '../../types/ast-extensions.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { GeneratorCapabilities, GeneratorCapabilitiesFactory } from '../common/generator-capabilities.js';
import { getEntities } from '../../utils/aggregate-helpers.js';

export class EventProcessingGenerator {
    private capabilities: GeneratorCapabilities;

    constructor(capabilities?: GeneratorCapabilities) {
        this.capabilities = capabilities || GeneratorCapabilitiesFactory.createWebApiCapabilities();
    }
    // Helper methods migrated from OrchestrationBase
    private capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    private getBasePackage(): string {
        return this.capabilities.packageBuilder.buildCustomPackage('').split('.').slice(0, -1).join('.');
    }

    private getTransactionModel(): string {
        return 'SAGAS';
    }

    private renderSimpleTemplate(template: string, context: any): string {
        let result = template;

        result = result.replace(/\{\{(\w+)\}\}/g, (match, key) => {
            return context[key] || match;
        });

        result = result.replace(/\{\{#each (\w+)\}\}([\s\S]*?)\{\{\/each\}\}/g, (match, arrayKey, content) => {
            const array = context[arrayKey];
            if (!Array.isArray(array)) return '';

            return array.map(item => {
                let itemContent = content;
                Object.keys(item).forEach(key => {
                    const regex = new RegExp(`\\{\\{${key}\\}\\}`, 'g');
                    itemContent = itemContent.replace(regex, item[key]);
                });
                return itemContent;
            }).join('');
        });

        return result;
    }

    async generate(aggregate: AggregateExt, rootEntity: EntityExt, options: CoordinationGenerationOptions, allAggregates?: AggregateExt[]): Promise<string> {
        const context = this.buildContext(aggregate, rootEntity, options, allAggregates);
        // Check if there are methods before building template
        const hasMethods = context.eventProcessingMethods !== undefined && context.eventProcessingMethods.trim().length > 0;
        const template = this.buildTemplateString(context, hasMethods);
        return this.renderSimpleTemplate(template, context);
    }

    private buildContext(aggregate: AggregateExt, rootEntity: EntityExt, options: CoordinationGenerationOptions, allAggregates?: AggregateExt[]): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const projectName = options.projectName.toLowerCase();
        const ProjectName = this.capitalize(options.projectName);

        const eventProcessingMethodsArray = this.buildEventProcessingMethods(aggregate, rootEntity, capitalizedAggregate);
        const imports = this.buildImports(aggregate, options, allAggregates);

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
            hasSagas: options.architecture === 'causal-saga'
        };

        const renderedMethods = eventProcessingMethodsArray.map((method: any) => this.renderMethod(method, tempContext, aggregate)).join('\n\n');

        return {
            ...tempContext,
            eventProcessingMethods: renderedMethods.trim().length > 0 ? renderedMethods : undefined
        };
    }

    private buildEventProcessingMethods(aggregate: AggregateExt, rootEntity: EntityExt, aggregateName: string): any[] {
        const methods: any[] = [];

        // Collect all subscribed events (direct + interInvariants)
        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);

        // Filter out inter-invariant subscriptions as they don't have handler methods
        const eventsWithHandlers = allSubscribedEvents.filter((event: any) => !event.isInterInvariant);

        eventsWithHandlers.forEach((event: any) => {
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
    private collectSubscribedEvents(aggregate: AggregateExt): any[] {
        const aggregateEvents = (aggregate as any).events;
        if (!aggregateEvents) {
            return [];
        }

        const directSubscribed = aggregateEvents.subscribedEvents || [];
        const interSubscribed = aggregateEvents.interInvariants?.flatMap((ii: any) =>
            (ii?.subscribedEvents || []).map((sub: any) => ({ ...sub, isInterInvariant: true }))
        ) || [];
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

    /**
     * Find which aggregate publishes a given event by checking all aggregates' published events.
     * Handles both custom published events and auto-generated CRUD events.
     */
    private findEventPublisher(eventTypeName: string, allAggregates: AggregateExt[]): string | null {
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

    private buildImports(aggregate: AggregateExt, options: CoordinationGenerationOptions, allAggregates?: AggregateExt[]): string[] {
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

        // Filter out inter-invariant subscriptions as they don't have handler methods
        const eventsWithHandlers = allSubscribedEvents.filter((event: any) => !event.isInterInvariant);

        eventsWithHandlers.forEach((event: any) => {
            const eventTypeName = event.eventType || 'UnknownEvent';
            let sourceAggregateName = 'unknown';

            // PRIORITY 1: Try AST reference (works for custom events with explicit references)
            const publishedEvent = event.eventType?.ref as any;
            const eventsContainer = publishedEvent?.$container as any;
            const sourceAggregate = eventsContainer?.$container as any;

            if (sourceAggregate?.name) {
                sourceAggregateName = sourceAggregate.name.toLowerCase();
            }
            // PRIORITY 2: Search all aggregates for event publisher (works for CRUD events)
            else if (allAggregates && allAggregates.length > 0) {
                const found = this.findEventPublisher(eventTypeName, allAggregates);
                if (found) {
                    sourceAggregateName = found;
                } else {
                    console.warn(`Warning: Could not find publisher aggregate for event ${eventTypeName}`);
                    // Fallback to regex as last resort
                    sourceAggregateName = eventTypeName
                        .replace(/(Updated|Deleted|Created)Event$/, '')
                        .toLowerCase();
                }
            }
            // PRIORITY 3: Fallback to regex (only when allAggregates not available)
            else {
                console.warn(`Warning: allAggregates not available, using fallback for ${eventTypeName}`);
                sourceAggregateName = eventTypeName
                    .replace(/(Updated|Deleted|Created)Event$/, '')
                    .toLowerCase();
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

    private renderMethod(method: any, context: any, aggregate: AggregateExt): string {
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

    private deriveServiceMethodName(eventTypeName: string, aggregate: AggregateExt): string {
        // Use handle{EventName} pattern (e.g., UserDeletedEvent -> handleUserDeletedEvent)
        return `handle${eventTypeName}`;
    }

    private buildServiceMethodParams(eventTypeName: string, eventVarName: string, aggregateIdName: string, aggregate: AggregateExt, subscribedEvent: any): string {
        const nameWithoutEvent = eventTypeName.replace(/Event$/, '');
        const isUpdate = nameWithoutEvent.endsWith('Updated');
        const isDelete = nameWithoutEvent.endsWith('Deleted');

        // Extract publisher aggregate name from event
        let publisherAggregateName = nameWithoutEvent.replace(/(Updated|Deleted|Removed|Created)$/, '');

        // Check if there's a projection entity that references the publisher aggregate
        const entities = getEntities(aggregate);
        const matchingProjection = entities.find((e: any) => {
            const aggregateRef = (e as any).aggregateRef;
            return !e.isRoot && aggregateRef && aggregateRef.toLowerCase() === publisherAggregateName.toLowerCase();
        });

        // For Update events, pass aggregateId, publisherAggregateId, publisherAggregateVersion, and primitive fields from event
        if (isUpdate) {
            const fieldParams: string[] = [];

            // Use Event base class methods
            fieldParams.push(`${eventVarName}.getPublisherAggregateId()`);
            fieldParams.push(`${eventVarName}.getPublisherAggregateVersion()`);

            // Extract primitive fields from the projection entity's field mappings (if exists)
            // ONLY extract from fieldMappings (mapped properties from referenced aggregate)
            // DO NOT extract local properties - those don't exist in the incoming event!
            if (matchingProjection) {
                const fieldMappings = (matchingProjection as any).fieldMappings || [];
                const aggregateRef = (matchingProjection as any).aggregateRef;

                // Determine the correct field names to use for event method calls
                // Pattern matching for projection entity names like "ExecutionUser", "AnswerQuestion", etc.
                // If aggregateRef matches pattern [Aggregate][Entity] (e.g., "ExecutionUser"),
                // the event uses entity field names with lowercase [Entity] prefix (e.g., "userName")
                // Otherwise (simple names like "User"), the event uses DTO field names (e.g., "name")

                let fieldPrefix = '';
                // Check if aggregateRef looks like a projection (multiple capital letters)
                const projectionPattern = /^([A-Z][a-z]+)([A-Z][a-z]+)$/;
                const match = aggregateRef.match(projectionPattern);

                if (match) {
                    // It's a projection like "ExecutionUser" -> prefix is "user"
                    fieldPrefix = match[2].toLowerCase();
                }

                for (const mapping of fieldMappings) {
                    const dtoField = mapping.dtoField;

                    // Skip system fields
                    if (dtoField.endsWith('AggregateId') || dtoField.endsWith('Version') || dtoField.endsWith('State')) {
                        continue;
                    }

                    // Build field name: if prefix exists, use prefix + dtoField, otherwise just dtoField
                    const fieldName = fieldPrefix ? fieldPrefix + this.capitalize(dtoField) : dtoField;
                    const capitalizedField = this.capitalize(fieldName);
                    fieldParams.push(`${eventVarName}.get${capitalizedField}()`);
                }
            }

            fieldParams.push('unitOfWork');
            return `${aggregateIdName}, ${fieldParams.join(', ')}`;
        } else if (isDelete) {
            // For Delete events: aggregateId, publisherAggregateId, publisherAggregateVersion, unitOfWork
            return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
        }

        // Default fallback
        return `${aggregateIdName}, unitOfWork`;
    }


}
