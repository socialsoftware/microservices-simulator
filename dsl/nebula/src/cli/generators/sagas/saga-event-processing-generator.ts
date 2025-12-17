import { OrchestrationBase } from '../common/orchestration-base.js';
import { initializeAggregateProperties, getWorkflows } from '../../utils/aggregate-helpers.js';
import { SagaHelpers } from './saga-helpers.js';
import { SagaWorkflowGenerator } from './saga-workflow-generator.js';

/**
 * Generates saga functionality classes for event processing workflows
 */
export class SagaEventProcessingGenerator extends OrchestrationBase {
    private helpers = new SagaHelpers();
    private workflowGenerator = new SagaWorkflowGenerator();

    /**
     * Generate saga functionalities for event processing
     */
    generateEventProcessingSagaFunctionalities(aggregate: any, options: { projectName: string }, outputs: Record<string, string>): void {
        // Ensure aggregate properties are initialized
        initializeAggregateProperties(aggregate);

        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const packageName = `${basePackage}.${options.projectName.toLowerCase()}.sagas.coordination.${lowerAggregate}`;
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);

        // Get all subscribed events
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

        // Deduplicate events by event type name
        const eventMap = new Map<string, any>();
        allSubscribedEvents.forEach((event: any) => {
            const eventTypeName = event.eventType?.ref?.name || event.eventType?.$refText || 'UnknownEvent';
            if (!eventMap.has(eventTypeName)) {
                eventMap.set(eventTypeName, event);
            }
        });

        // Match events to saga workflows and generate classes
        for (const [eventTypeName, subscribedEvent] of eventMap.entries()) {
            const sagaWorkflow = this.findSagaWorkflowForEvent(aggregate, eventTypeName);
            if (!sagaWorkflow) {
                continue;
            }

            // Use event name for class name, not workflow name
            const eventNameWithoutEvent = eventTypeName.replace(/Event$/, '');
            const className = `${this.capitalize(eventNameWithoutEvent)}${capitalizedAggregate}FunctionalitySagas`;

            // Skip if already generated as business workflow
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

    /**
     * Generate a single event processing saga class
     */
    private generateEventProcessingSagaClass(
        aggregate: any,
        subscribedEvent: any,
        sagaWorkflow: any,
        eventTypeName: string,
        className: string,
        packageName: string,
        options: { projectName: string }
    ): string {
        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = this.capitalize(aggregate.name);

        const imports: string[] = [];
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${capitalizedAggregate}Service;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaSyncStep;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);
        imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);

        // Add imports for workflow steps with dependencies
        const workflowSteps = sagaWorkflow.workflowSteps || [];
        const hasStepDependencies = workflowSteps.some((s: any) => s.dependsOn?.dependencies?.length > 0);
        if (hasStepDependencies) {
            imports.push('import java.util.ArrayList;');
            imports.push('import java.util.Arrays;');
        }

        // Add saga state imports if workflow uses registerState
        const usesRegisterState = workflowSteps.some((s: any) =>
            (s.stepActions || []).some((a: any) => a.$type === 'WorkflowRegisterStateAction') ||
            (s.compensation?.compensationActions || []).some((a: any) => a.$type === 'WorkflowRegisterStateAction')
        );
        if (usesRegisterState) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${capitalizedAggregate}SagaState;`);
            imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
        }

        // Add DTO import if workflow uses fields with DTOs
        const workflowFields = sagaWorkflow.workflowFields || [];
        if (workflowFields.length > 0) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos.Saga${capitalizedAggregate}Dto;`);
        }

        // Determine the actual source aggregate for the event
        let sourceAggregateName = 'unknown';
        const publishedEvent = subscribedEvent.eventType?.ref as any;
        const eventsContainer = publishedEvent?.$container as any;
        const sourceAggregate = eventsContainer?.$container as any;
        if (sourceAggregate?.name) {
            sourceAggregateName = sourceAggregate.name.toLowerCase();
        }

        // Add event import
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${sourceAggregateName}.events.publish.${eventTypeName};`);

        // Build constructor parameters
        const workflowParams = sagaWorkflow.parameters || [];
        const constructorParams: Array<{ type: string; name: string }> = [
            { type: `${capitalizedAggregate}Service`, name: `${lowerAggregate}Service` },
            { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' },
            { type: 'Integer', name: 'aggregateId' } // Subscriber aggregate ID
        ];

        // Extract event fields from the PublishedEvent
        const eventRef = subscribedEvent.eventType?.ref as any;
        const eventFields = eventRef?.fields || [];

        // Add standard event fields first (always present in Event base class)
        constructorParams.push({ type: 'Integer', name: 'publisherAggregateId' });

        // Add custom event fields, mapping them correctly
        eventFields.forEach((field: any) => {
            const fieldName = field.name || String(field);
            const fieldType = typeof field.type === 'string' ? field.type : (field.type?.typeName || field.type?.name || 'Object');

            // Map field names to match getter methods used in EventProcessing
            let paramName = fieldName;
            if (eventTypeName.includes('Anonymize') && fieldName === 'userAggregateId') {
                paramName = 'studentAggregateId';
            } else if (eventTypeName.includes('UpdateStudentName') && fieldName === 'newName') {
                paramName = 'updatedName';
            } else if (eventTypeName.includes('UpdateStudentName') && fieldName === 'oldName') {
                return; // Skip oldName - not used in constructor
            }
            constructorParams.push({ type: fieldType, name: paramName });
        });

        // Add publisher version (standard Event field)
        constructorParams.push({ type: 'Integer', name: 'publisherAggregateVersion' });
        constructorParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });

        // Build buildWorkflow parameters
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

            // Map workflow parameter to constructor parameter
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

        // Add unitOfWork to buildWorkflow call if workflow needs it
        if (workflowParams.some((p: any) => {
            const pType = typeof p.type === 'string' ? p.type : (p.type?.typeName || p.type?.name || '');
            return pType === 'UnitOfWork';
        })) {
            buildParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });
        }

        // Generate workflow steps
        const stepsBody = this.workflowGenerator.generateEventProcessingWorkflowSteps(
            sagaWorkflow.name, aggregate.name, lowerAggregate, workflowParams, sagaWorkflow
        );

        // Build constructor body
        const fieldsToStore = constructorParams.filter(p => p.name !== 'unitOfWork');
        const constructorBody = fieldsToStore.map(p => `        this.${p.name} = ${p.name};`).join('\n');
        const constructorBodyWithBuildWorkflow = constructorBody + `\n        this.buildWorkflow(${buildWorkflowCallParams.join(', ')});`;

        const buildParamsString = buildParams.map(p => `${p.type} ${p.name}`).join(', ');
        const constructorParamsString = constructorParams.map(p => `${p.type} ${p.name}`).join(', ');

        // Generate field declarations
        const fieldsString = fieldsToStore.map((p: { type: string; name: string }) => `    private ${p.type} ${p.name};`).join('\n');

        // Generate workflow field declarations
        const workflowFieldsString = workflowFields.map((f: any) => {
            const fieldType = this.helpers.getParamTypeName(f.type, capitalizedAggregate);
            return `    private ${fieldType} ${f.name};`;
        }).join('\n');

        const allFieldsString = [fieldsString, workflowFieldsString].filter(Boolean).join('\n');

        // Generate getters and setters for workflow fields
        const gettersSetters = workflowFields.map((f: any) => {
            const fieldType = this.helpers.getParamTypeName(f.type, capitalizedAggregate);
            const capitalizedName = this.capitalize(f.name);
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

    /**
     * Find a saga workflow that matches a given event type
     */
    findSagaWorkflowForEvent(aggregate: any, eventTypeName: string): any {
        // Ensure aggregate properties are initialized
        initializeAggregateProperties(aggregate);

        // Access workflows using the helper function - filter for saga workflows only
        const allWorkflows = getWorkflows(aggregate);

        const workflows = allWorkflows.filter((w: any) => {
            return w.workflowType === 'saga';
        });

        if (workflows.length === 0) {
            return null;
        }

        const eventNameWithoutEvent = eventTypeName.replace(/Event$/, '');
        const lowerEventName = eventNameWithoutEvent.toLowerCase();

        // Try to find workflow by name pattern
        return workflows.find((w: any) => {
            const workflowName = (w.name || String(w)).toLowerCase();

            // Normalize the event name
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

