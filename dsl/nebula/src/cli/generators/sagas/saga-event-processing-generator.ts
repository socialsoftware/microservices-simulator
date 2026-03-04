import { initializeAggregateProperties, getWorkflows } from '../../utils/aggregate-helpers.js';
import { SagaHelpers } from './saga-helpers.js';
import { SagaWorkflowGenerator } from './saga-workflow-generator.js';
import { SagaGenerationOptions } from './saga-generator.js';
import { StringUtils } from '../../utils/string-utils.js';
import { EventNameParser } from '../common/utils/event-name-parser.js';



export class SagaEventProcessingGenerator {
    private getBasePackage(options: SagaGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in SagaGenerationOptions');
        }
        return options.basePackage;
    }
    private helpers = new SagaHelpers();
    private workflowGenerator = new SagaWorkflowGenerator();

    

    generateEventProcessingSagaFunctionalities(aggregate: any, options: SagaGenerationOptions, outputs: Record<string, string>): void {
        
        initializeAggregateProperties(aggregate);

        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const packageName = `${basePackage}.${options.projectName.toLowerCase()}.sagas.coordination.${lowerAggregate}`;
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);

        
        const aggregateEvents = (aggregate as any).events;
        if (!aggregateEvents) {
            return;
        }

        const directSubscribed = aggregateEvents.subscribedEvents || [];
        const interSubscribed = aggregateEvents.interInvariants?.flatMap((ii: any) => ii?.subscribedEvents || []) || [];
        const allSubscribedEvents = [...directSubscribed, ...interSubscribed];

        if (allSubscribedEvents.length === 0) {
            return;
        }

        
        const eventMap = new Map<string, any>();
        allSubscribedEvents.forEach((event: any) => {
            const eventTypeName = event.eventType || 'UnknownEvent';
            if (!eventMap.has(eventTypeName)) {
                eventMap.set(eventTypeName, event);
            }
        });

        
        for (const [eventTypeName, subscribedEvent] of eventMap.entries()) {
            const sagaWorkflow = this.findSagaWorkflowForEvent(aggregate, eventTypeName);
            if (!sagaWorkflow) {
                continue;
            }

            
            const eventNameWithoutEvent = EventNameParser.removeEventSuffix(eventTypeName);
            const className = `${StringUtils.capitalize(eventNameWithoutEvent)}${capitalizedAggregate}FunctionalitySagas`;

            
            const existingFile = outputs[className + '.java'];
            if (existingFile) {
                const hasParamsInBuildWorkflow = existingFile.match(/buildWorkflow\s*\([^)]+\)/);
                if (!hasParamsInBuildWorkflow) {
                    continue;
                }
            }

            const content = this.generateEventProcessingSagaClass(
                aggregate, subscribedEvent, sagaWorkflow, eventTypeName,
                className, packageName, options
            );

            outputs[className + '.java'] = content;
        }
    }

    

    private generateEventProcessingSagaClass(
        aggregate: any,
        subscribedEvent: any,
        sagaWorkflow: any,
        eventTypeName: string,
        className: string,
        packageName: string,
        options: SagaGenerationOptions
    ): string {
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);

        const imports: string[] = [];
        imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.push(`import ${basePackage}.ms.coordination.workflow.command.CommandGateway;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.ServiceMapping;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.command.${lowerAggregate}.*;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaStep;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);

        
        const workflowSteps = sagaWorkflow.workflowSteps || [];
        const hasStepDependencies = workflowSteps.some((s: any) => s.dependsOn?.dependencies?.length > 0);
        if (hasStepDependencies) {
            imports.push('import java.util.ArrayList;');
            imports.push('import java.util.Arrays;');
        }

        
        const usesRegisterState = workflowSteps.some((s: any) =>
            (s.stepActions || []).some((a: any) => a.$type === 'WorkflowRegisterStateAction') ||
            (s.compensation?.compensationActions || []).some((a: any) => a.$type === 'WorkflowRegisterStateAction')
        );
        if (usesRegisterState) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${capitalizedAggregate}SagaState;`);
            imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
        }

        
        const workflowFields = sagaWorkflow.workflowFields || [];
        if (workflowFields.length > 0) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos.Saga${capitalizedAggregate}Dto;`);
        }

        
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.events.${eventTypeName};`);

        const workflowParams = sagaWorkflow.parameters || [];
        const constructorParams: Array<{ type: string; name: string }> = [
            { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' },
            { type: 'CommandGateway', name: 'commandGateway' },
            { type: 'Integer', name: 'aggregateId' }
        ];

        
        const eventRef = subscribedEvent.eventType?.ref as any;
        const eventFields = eventRef?.fields || [];

        
        constructorParams.push({ type: 'Integer', name: 'publisherAggregateId' });

        
        eventFields.forEach((field: any) => {
            const fieldName = field.name || String(field);
            const fieldType = typeof field.type === 'string' ? field.type : (field.type?.typeName || field.type?.name || 'Object');

            
            let paramName = fieldName;
            if (eventTypeName.includes('Anonymize') && fieldName === 'userAggregateId') {
                paramName = 'studentAggregateId';
            } else if (eventTypeName.includes('UpdateStudentName') && fieldName === 'newName') {
                paramName = 'updatedName';
            } else if (eventTypeName.includes('UpdateStudentName') && fieldName === 'oldName') {
                return; 
            }
            constructorParams.push({ type: fieldType, name: paramName });
        });

        
        constructorParams.push({ type: 'Integer', name: 'publisherAggregateVersion' });
        constructorParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });

        
        const buildParams: Array<{ type: string; name: string }> = [];
        const buildWorkflowCallParams: string[] = [];

        for (const workflowParam of workflowParams) {
            const originalParamName = workflowParam.name || String(workflowParam);
            const workflowParamName = originalParamName.toLowerCase();
            const workflowParamType = typeof workflowParam.type === 'string' ? workflowParam.type : (workflowParam.type?.typeName || workflowParam.type?.name || 'Object');

            if (workflowParamType === 'UnitOfWork') {
                buildWorkflowCallParams.push('unitOfWork');
                continue;
            }

            
            let constructorParamName = '';
            if (workflowParamName.includes('aggregateid') && workflowParamName !== 'aggregateid') {
                constructorParamName = 'aggregateId';
            } else if (workflowParamName.includes('student') || workflowParamName.includes('user')) {
                const matchingParam = constructorParams.find(p =>
                    p.name.toLowerCase().includes('student') ||
                    p.name.toLowerCase().includes('user') ||
                    (workflowParamName.includes('student') && p.name.toLowerCase() === 'studentaggregateid') ||
                    (workflowParamName.includes('user') && p.name.toLowerCase() === 'useraggregateid')
                );
                constructorParamName = matchingParam?.name || originalParamName;
            } else {
                const matchingParam = constructorParams.find(p =>
                    p.name.toLowerCase() === workflowParamName ||
                    p.name.toLowerCase().includes(workflowParamName) ||
                    workflowParamName.includes(p.name.toLowerCase())
                );
                constructorParamName = matchingParam?.name || originalParamName;
            }

            buildParams.push({ type: workflowParamType, name: constructorParamName });
            buildWorkflowCallParams.push(constructorParamName);
        }

        
        if (workflowParams.some((p: any) => {
            const pType = typeof p.type === 'string' ? p.type : (p.type?.typeName || p.type?.name || '');
            return pType === 'UnitOfWork';
        })) {
            buildParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });
        }

        
        const stepsBody = this.workflowGenerator.generateEventProcessingWorkflowSteps(
            sagaWorkflow.name, aggregate.name, lowerAggregate, workflowParams, sagaWorkflow
        );

        
        const fieldsToStore = constructorParams.filter(p => p.name !== 'unitOfWork');
        const constructorBody = fieldsToStore.map(p => `        this.${p.name} = ${p.name};`).join('\n');
        const constructorBodyWithBuildWorkflow = constructorBody + `\n        this.buildWorkflow(${buildWorkflowCallParams.join(', ')});`;

        const buildParamsString = buildParams.map(p => `${p.type} ${p.name}`).join(', ');
        const constructorParamsString = constructorParams.map(p => `${p.type} ${p.name}`).join(', ');

        
        const fieldsString = fieldsToStore.map((p: { type: string; name: string }) => `    private ${p.type} ${p.name};`).join('\n');

        
        const workflowFieldsString = workflowFields.map((f: any) => {
            const fieldType = this.helpers.getParamTypeName(f.type, capitalizedAggregate);
            return `    private ${fieldType} ${f.name};`;
        }).join('\n');

        const allFieldsString = [fieldsString, workflowFieldsString].filter(Boolean).join('\n');

        
        const gettersSetters = workflowFields.map((f: any) => {
            const fieldType = this.helpers.getParamTypeName(f.type, capitalizedAggregate);
            const capitalizedName = StringUtils.capitalize(f.name);
            return `
    public void set${capitalizedName}(${fieldType} ${f.name}) {
        this.${f.name} = ${f.name};
    }

    public ${fieldType} get${capitalizedName}() {
        return ${f.name};
    }`;
        }).join('\n');

        return `package ${packageName};

${imports.map(i => i).join('\n')}

public class ${className} extends WorkflowFunctionality {
${allFieldsString}

    public ${className}(${constructorParamsString}) {
${constructorBodyWithBuildWorkflow}
    }

    public void buildWorkflow(${buildParamsString}) {
${stepsBody}
    }
${gettersSetters}
}`;
    }

    

    findSagaWorkflowForEvent(aggregate: any, eventTypeName: string): any {
        
        initializeAggregateProperties(aggregate);

        
        const allWorkflows = getWorkflows(aggregate);

        const workflows = allWorkflows.filter((w: any) => {
            return w.workflowType === 'saga';
        });

        if (workflows.length === 0) {
            return null;
        }

        const eventNameWithoutEvent = eventTypeName.replace(/Event$/, '');
        const lowerEventName = eventNameWithoutEvent.toLowerCase();

        
        return workflows.find((w: any) => {
            const workflowName = (w.name || String(w)).toLowerCase();

            
            const normalizedEventName = lowerEventName
                .replace('delete', 'remove')
                .replace('disenroll', 'unenroll');

            const withStudentToUser = normalizedEventName.replace('student', 'user');
            const withUserToStudent = normalizedEventName.replace('user', 'student');

            return workflowName === lowerEventName ||
                workflowName === normalizedEventName ||
                workflowName === withStudentToUser ||
                workflowName === withUserToStudent ||
                workflowName === lowerEventName.replace('student', 'user') ||
                workflowName === lowerEventName.replace('user', 'student');
        });
    }
}

