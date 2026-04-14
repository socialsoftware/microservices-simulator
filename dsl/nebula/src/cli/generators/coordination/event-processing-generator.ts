
import { AggregateExt, EntityExt } from '../../types/ast-extensions.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { GeneratorCapabilities, GeneratorCapabilitiesFactory } from '../common/generator-capabilities.js';
import { getEntities, getEffectiveFieldMappings, dtoFieldToString } from '../../utils/aggregate-helpers.js';
import { StringUtils } from '../../utils/string-utils.js';
import { EventNameParser } from '../common/utils/event-name-parser.js';
import { UnifiedTypeResolver as TypeResolver } from '../common/unified-type-resolver.js';

export class EventProcessingGenerator {
    private capabilities: GeneratorCapabilities;

    constructor(capabilities?: GeneratorCapabilities) {
        this.capabilities = capabilities || GeneratorCapabilitiesFactory.createWebApiCapabilities();
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

        const hasMethods = context.eventProcessingMethods !== undefined && context.eventProcessingMethods.trim().length > 0;
        const template = this.buildTemplateString(context, hasMethods);
        return this.renderSimpleTemplate(template, context);
    }

    private buildContext(aggregate: AggregateExt, rootEntity: EntityExt, options: CoordinationGenerationOptions, allAggregates?: AggregateExt[]): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const projectName = options.projectName.toLowerCase();
        const ProjectName = StringUtils.capitalize(options.projectName);

        const eventProcessingMethodsArray = this.buildEventProcessingMethods(aggregate, rootEntity, capitalizedAggregate);
        const imports = this.buildImports(aggregate, options, allAggregates);

        const basePackage = this.getBasePackage();
        const references = (aggregate as any).references;
        const allEvents = this.collectSubscribedEvents(aggregate);
        const hasInterInvariantDeleted = allEvents.some((event: any) => {
            if (!event.isInterInvariant) return false;
            const nameWithoutEvent = EventNameParser.removeEventSuffix(event.eventType || '');
            return nameWithoutEvent.endsWith('Deleted');
        });
        const hasReferenceConstraints = (references?.constraints && references.constraints.length > 0) || hasInterInvariantDeleted;

        const tempContext = {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${basePackage}.${projectName}.microservices.${lowerAggregate}.coordination.eventProcessing`,
            basePackage,
            transactionModel: this.getTransactionModel(),
            imports: imports.join('\n'),
            projectName,
            ProjectName,
            hasSagas: true,
            hasReferenceConstraints
        };

        const renderedMethods = eventProcessingMethodsArray.map((method: any) => this.renderMethod(method, tempContext, aggregate)).join('\n\n');

        return {
            ...tempContext,
            eventProcessingMethods: renderedMethods.trim().length > 0 ? renderedMethods : undefined
        };
    }

    private buildEventProcessingMethods(aggregate: AggregateExt, rootEntity: EntityExt, aggregateName: string): any[] {
        const methods: any[] = [];


        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);
        const addedEventTypes = new Set<string>();

        for (const event of allSubscribedEvents) {
            const eventTypeName = event.eventType || 'UnknownEvent';
            if (addedEventTypes.has(eventTypeName)) continue;
            addedEventTypes.add(eventTypeName);

            const nameWithoutEvent = EventNameParser.removeEventSuffix(eventTypeName);
            const isDeleted = nameWithoutEvent.endsWith('Deleted');

            if (event.isInterInvariant && isDeleted) {
                methods.push({
                    name: `process${eventTypeName}`,
                    returnType: 'void',
                    isReferenceConstraint: true,
                    parameters: [
                        { name: 'aggregateId', type: 'Integer' },
                        { name: eventTypeName.charAt(0).toLowerCase() + eventTypeName.slice(1), type: eventTypeName }
                    ]
                });
            } else {
                methods.push({
                    name: `process${eventTypeName}`,
                    returnType: 'void',
                    parameters: [
                        { name: 'aggregateId', type: 'Integer' },
                        { name: eventTypeName.charAt(0).toLowerCase() + eventTypeName.slice(1), type: eventTypeName }
                    ]
                });
            }
        }

        const references = (aggregate as any).references;
        if (references?.constraints) {
            for (const constraint of references.constraints) {
                const targetAggregate = constraint.targetAggregate;
                const eventTypeName = `${targetAggregate}DeletedEvent`;
                if (addedEventTypes.has(eventTypeName)) continue;
                addedEventTypes.add(eventTypeName);
                methods.push({
                    name: `process${eventTypeName}`,
                    returnType: 'void',
                    isReferenceConstraint: true,
                    parameters: [
                        { name: 'aggregateId', type: 'Integer' },
                        { name: eventTypeName.charAt(0).toLowerCase() + eventTypeName.slice(1), type: eventTypeName }
                    ]
                });
            }
        }

        return methods;
    }



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


        const eventMap = new Map<string, any>();
        allSubscribed.forEach((event: any) => {
            const eventTypeName = event.eventType || 'UnknownEvent';
            if (!eventMap.has(eventTypeName)) {
                eventMap.set(eventTypeName, event);
            }
        });

        return Array.from(eventMap.values());
    }



    private buildImports(aggregate: AggregateExt, options: CoordinationGenerationOptions, allAggregates?: AggregateExt[]): string[] {
        const imports: string[] = [];
        const projectName = options.projectName.toLowerCase();
        const basePackage = this.getBasePackage();

        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.stereotype.Service;');
        imports.push(`import ${basePackage}.ms.coordination.unitOfWork.UnitOfWork;`);
        imports.push(`import ${basePackage}.ms.coordination.unitOfWork.UnitOfWorkService;`);
        imports.push(`import ${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.service.${StringUtils.capitalize(aggregate.name)}Service;`);


        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);

        allSubscribedEvents.forEach((event: any) => {
            const eventTypeName = event.eventType || 'UnknownEvent';
            const importLine = `import ${basePackage}.${projectName}.events.${eventTypeName};`;
            if (!imports.includes(importLine)) {
                imports.push(importLine);
            }
        });

        const hasInterInvariantDeleted = allSubscribedEvents.some((event: any) => {
            if (!event.isInterInvariant) return false;
            const nameWithoutEvent = EventNameParser.removeEventSuffix(event.eventType || '');
            return nameWithoutEvent.endsWith('Deleted');
        });
        if (hasInterInvariantDeleted) {
            const lowerAgg = aggregate.name.toLowerCase();
            const capAgg = StringUtils.capitalize(aggregate.name);
            const aggImport = `import ${basePackage}.${projectName}.microservices.${lowerAgg}.aggregate.${capAgg};`;
            const factoryImport = `import ${basePackage}.${projectName}.microservices.${lowerAgg}.aggregate.${capAgg}Factory;`;
            const baseAggImport = `import ${basePackage}.ms.domain.aggregate.Aggregate;`;
            if (!imports.includes(aggImport)) imports.push(aggImport);
            if (!imports.includes(factoryImport)) imports.push(factoryImport);
            if (!imports.includes(baseAggImport)) imports.push(baseAggImport);
        }

        const references = (aggregate as any).references;
        if (references?.constraints && references.constraints.length > 0) {
            const lowerAgg = aggregate.name.toLowerCase();
            const capAgg = StringUtils.capitalize(aggregate.name);
            const aggImport = `import ${basePackage}.${projectName}.microservices.${lowerAgg}.aggregate.${capAgg};`;
            const factoryImport = `import ${basePackage}.${projectName}.microservices.${lowerAgg}.aggregate.${capAgg}Factory;`;
            const baseAggImport = `import ${basePackage}.ms.domain.aggregate.Aggregate;`;
            if (!imports.includes(aggImport)) imports.push(aggImport);
            if (!imports.includes(factoryImport)) imports.push(factoryImport);
            if (!imports.includes(baseAggImport)) imports.push(baseAggImport);

            for (const constraint of references.constraints) {
                const targetAggregate = constraint.targetAggregate;
                const eventTypeName = `${targetAggregate}DeletedEvent`;
                const importLine = `import ${basePackage}.${projectName}.events.${eventTypeName};`;
                if (!imports.includes(importLine)) {
                    imports.push(importLine);
                }
            }
        }

        return imports;
    }

    private buildTemplateString(context: any, hasMethods: boolean): string {

        const methodsSection = hasMethods ? '\n\n{{eventProcessingMethods}}\n' : '';

        return `package {{packageName}};

{{imports}}

@Service
public class {{aggregateName}}EventProcessing {
    @Autowired
    private {{aggregateName}}Service {{lowerAggregate}}Service;
${context.hasReferenceConstraints ? `
    @Autowired
    private ${context.aggregateName}Factory ${context.lowerAggregate}Factory;
` : ''}
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public {{aggregateName}}EventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }${methodsSection}}`;
    }

    private renderMethod(method: any, context: any, aggregate: AggregateExt): string {
        const params = method.parameters.map((p: any) => `${p.type} ${p.name}`).join(', ');

        if (method.isReferenceConstraint) {
            const lowerAggregate = context.lowerAggregate || context.aggregateName?.toLowerCase() || 'aggregate';
            const capAggregate = context.aggregateName || 'Aggregate';
            return `    public ${method.returnType} ${method.name}(${params}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        ${capAggregate} old${capAggregate} = (${capAggregate}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        ${capAggregate} new${capAggregate} = ${lowerAggregate}Factory.create${capAggregate}FromExisting(old${capAggregate});
        new${capAggregate}.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(new${capAggregate}, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }`;
        }

        const eventParam = method.parameters.find((p: any) => p.type !== 'Integer' || p.name !== 'aggregateId');
        const eventTypeName = eventParam ? eventParam.type : 'UnknownEvent';
        const eventVarName = eventParam ? eventParam.name : 'event';
        const aggregateIdParam = method.parameters.find((p: any) => p.name === 'aggregateId');
        const aggregateIdName = aggregateIdParam ? aggregateIdParam.name : 'aggregateId';


        const allSubscribedEvents = this.collectSubscribedEvents(aggregate);
        const subscribedEvent = allSubscribedEvents.find((e: any) => {
            const eTypeName = e.eventType || 'UnknownEvent';
            return eTypeName === eventTypeName;
        });


        const serviceMethodName = this.deriveServiceMethodName(eventTypeName, aggregate);


        const serviceCallParams = this.buildServiceMethodParams(eventTypeName, eventVarName, aggregateIdName, aggregate, subscribedEvent || {});

        return `    public ${method.returnType} ${method.name}(${params}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        ${context.lowerAggregate}Service.${serviceMethodName}(${serviceCallParams});
        unitOfWorkService.commit(unitOfWork);
    }`;
    }

    private deriveServiceMethodName(eventTypeName: string, aggregate: AggregateExt): string {

        return `handle${eventTypeName}`;
    }

    private buildServiceMethodParams(eventTypeName: string, eventVarName: string, aggregateIdName: string, aggregate: AggregateExt, subscribedEvent: any): string {
        const nameWithoutEvent = EventNameParser.removeEventSuffix(eventTypeName);
        const isUpdate = nameWithoutEvent.endsWith('Updated');
        const isDelete = nameWithoutEvent.endsWith('Deleted');


        let publisherAggregateName = nameWithoutEvent.replace(/(Updated|Deleted|Removed|Created)$/, '');


        const entities = getEntities(aggregate);
        const matchingProjection = entities.find((e: any) => {
            const aggregateRef = (e as any).aggregateRef;
            return !e.isRoot && aggregateRef && aggregateRef.toLowerCase() === publisherAggregateName.toLowerCase();
        });


        if (isUpdate) {
            const fieldParams: string[] = [];


            fieldParams.push(`${eventVarName}.getPublisherAggregateId()`);
            fieldParams.push(`${eventVarName}.getPublisherAggregateVersion()`);




            if (matchingProjection) {

                const entityToExtractFrom = matchingProjection;


                const fieldMappings = getEffectiveFieldMappings(entityToExtractFrom);
                const aggregateRef = (matchingProjection as any).aggregateRef;







                let fieldPrefix = '';

                const projectionPattern = /^([A-Z][a-z]+)([A-Z][a-z]+)$/;
                const match = aggregateRef.match(projectionPattern);

                if (match) {

                    fieldPrefix = match[2].toLowerCase();
                }

                for (const mapping of fieldMappings) {
                    let dtoField = dtoFieldToString(mapping.dtoField);

                    const lowerDtoField = dtoField.toLowerCase();
                    if (lowerDtoField.endsWith('aggregateid') || lowerDtoField.endsWith('version') || lowerDtoField.endsWith('state')) {
                        continue;
                    }

                    if (mapping.type) {
                        const javaType = TypeResolver.resolveJavaType(mapping.type);
                        if (javaType === 'Object' || !javaType) {
                            continue;
                        }
                    } else {
                        continue;
                    }




                    const fieldName = fieldPrefix ? fieldPrefix + StringUtils.capitalize(dtoField) : dtoField;
                    const capitalizedField = StringUtils.capitalize(fieldName);
                    fieldParams.push(`${eventVarName}.get${capitalizedField}()`);
                }
            }

            fieldParams.push('unitOfWork');
            return `${aggregateIdName}, ${fieldParams.join(', ')}`;
        } else if (isDelete) {

            return `${aggregateIdName}, ${eventVarName}.getPublisherAggregateId(), ${eventVarName}.getPublisherAggregateVersion(), unitOfWork`;
        }


        return `${aggregateIdName}, unitOfWork`;
    }


}
