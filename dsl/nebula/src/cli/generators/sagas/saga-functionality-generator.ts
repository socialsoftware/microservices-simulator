import { OrchestrationBase } from '../common/orchestration-base.js';
import { initializeAggregateProperties, getWorkflows } from '../../utils/aggregate-helpers.js';

export class SagaFunctionalityGenerator extends OrchestrationBase {
    generateForAggregate(aggregate: any, options: { projectName: string }): Record<string, string> {
        const outputs: Record<string, string> = {};
        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const packageName = `${basePackage}.${options.projectName.toLowerCase()}.sagas.coordination.${lowerAggregate}`;

        // Get all workflows to check for matching definitions with step bodies
        initializeAggregateProperties(aggregate);
        const allWorkflows = getWorkflows(aggregate);

        if (aggregate.webApiEndpoints?.generateCrud) {
            const crudSagas = this.generateCrudSagaFunctionalities(aggregate, options, packageName);
            Object.assign(outputs, crudSagas);
        }

        const endpoints = aggregate.webApiEndpoints?.endpoints || [];
        for (const endpoint of endpoints) {
            const methodName: string = endpoint.methodName;
            if (!methodName) continue;

            const className = `${this.capitalize(methodName)}FunctionalitySagas`;

            // Check if there's a workflow with step definitions for this endpoint
            const matchingWorkflow = allWorkflows.find((w: any) =>
                w.name === methodName && w.workflowSteps && w.workflowSteps.length > 0
            );

            if (matchingWorkflow) {
                // Generate using the workflow definition with steps
                const content = this.generateWorkflowFunctionality(aggregate, matchingWorkflow, options, packageName);
                outputs[className + '.java'] = content;
                continue;
            }

            // Otherwise, generate basic functionality from endpoint
            const imports: string[] = [];
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${this.capitalize(aggregate.name)}Service;`);
            const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name };
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);

            const constructorDependencies = [
                { type: `${this.capitalize(aggregate.name)}Service`, name: `${lowerAggregate}Service` },
                { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' },
                { type: 'SagaUnitOfWork', name: 'unitOfWork' }
            ];

            let resultType: string | undefined;
            if (endpoint.returnType) {
                const rt = endpoint.returnType;
                if (typeof rt === 'string') {
                    if (rt === 'void') {
                        resultType = undefined;
                    } else if (rt.includes('Dto')) {
                        resultType = rt;
                    } else {
                        resultType = `${rootEntity.name}Dto`;
                    }
                } else if (rt.name) {
                    resultType = `${rootEntity.name}Dto`;
                }
            }

            const context = {
                packageName,
                imports,
                className,
                constructorDependencies,
                buildParams: [],
                resultType,
                stepsBody: ''
            };

            const template = this.loadTemplate('saga/functionality.hbs');
            const content = this.renderTemplate(template, context);
            outputs[className + '.java'] = content;
        }

        const functionalityBlock = (aggregate as any).functionalities;
        const functionalityMethods = functionalityBlock?.functionalityMethods || [];
        for (const func of functionalityMethods) {
            const methodName: string = func.name;
            if (!methodName) continue;

            const className = `${this.capitalize(methodName)}FunctionalitySagas`;
            const imports: string[] = [];
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${this.capitalize(aggregate.name)}Service;`);
            const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name };
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);

            const constructorDependencies: Array<{ type: string; name: string }> = [
                { type: `${this.capitalize(aggregate.name)}Service`, name: `${lowerAggregate}Service` },
                { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' },
                { type: 'SagaUnitOfWork', name: 'unitOfWork' }
            ];

            if (func.dependencyRefs && Array.isArray(func.dependencyRefs)) {
                for (const dep of func.dependencyRefs) {
                    const depType = String(dep);
                    const depName = depType.charAt(0).toLowerCase() + depType.slice(1);
                    constructorDependencies.push({ type: depType, name: depName });
                }
            }

            let resultType: string | undefined;
            if (func.returnType) {
                const rt = func.returnType as any;
                if (typeof rt === 'string') {
                    if (rt === 'void') {
                        resultType = undefined;
                    } else if (rt.includes('Dto')) {
                        resultType = rt;
                    } else {
                        resultType = `${rootEntity.name}Dto`;
                    }
                } else if (rt.name) {
                    resultType = `${rootEntity.name}Dto`;
                }
            }

            const buildParams: Array<{ type: string; name: string }> = [];
            if (func.parameters && Array.isArray(func.parameters)) {
                for (const p of func.parameters) {
                    const pName = p.name ?? String(p);
                    let pType = typeof p.type === 'string' ? p.type : (p.type?.typeName || p.type?.name || 'Object');
                    buildParams.push({ type: pType, name: pName });
                }
            }

            let stepsBody = '';
            const stepMap = new Map<string, string>(); // Map step names to variable names
            const steps = (func as any).functionalitySteps || [];
            const variableDeclarations: string[] = [];

            // First pass: collect all step names
            for (const s of steps) {
                const stepName = s.stepName || 'step';
                stepMap.set(stepName, `${stepName}Step`);
            }

            // Second pass: generate step code
            for (const s of steps) {
                const stepName = s.stepName || 'step';
                const stepVar = stepMap.get(stepName)!;
                const actions = s.stepActions || [];
                const dependencies = s.dependsOn?.dependencies || [];
                const compensation = s.compensation;

                // Generate step body actions
                let stepBody = '';

                for (const action of actions) {
                    if (action.$type === 'FuncCallServiceAction') {
                        const serviceRef = action.serviceRef;
                        const method = action.method;
                        const args = (action.args || []).map((arg: any) => this.convertStepArgument(arg)).join(', ');
                        const callExpr = `this.${serviceRef}.${method}(${args})`;

                        if (action.assignTo) {
                            // Assignment to variable
                            const varName = action.assignTo;
                            const varType = this.inferVariableType(method, resultType);
                            const decl = `private ${varType} ${varName};`;
                            if (!variableDeclarations.some(d => d.includes(varName))) {
                                variableDeclarations.push(decl);
                            }
                            stepBody += `            ${varName} = ${callExpr};\n`;
                        } else {
                            stepBody += `            ${callExpr};\n`;
                        }
                    } else if (action.$type === 'FuncSetResultAction') {
                        const resultTypeName = action.resultType || resultType || 'Object';
                        const getterName = action.getterName;
                        if (getterName) {
                            stepBody += `            this.result = ${resultTypeName}.${getterName}();\n`;
                        } else {
                            stepBody += `            this.result = ${resultTypeName};\n`;
                        }
                    } else if (action.$type === 'FuncAssignVariableAction') {
                        const varName = action.variableName;
                        const expr = this.convertStepExpression(action.expression);
                        const varType = this.inferVariableTypeFromExpression(expr);
                        if (!variableDeclarations.includes(varName)) {
                            variableDeclarations.push(`private ${varType} ${varName};`);
                        }
                        stepBody += `            ${varName} = ${expr};\n`;
                    } else if (action.$type === 'FuncRegisterSagaStateAction') {
                        const aggId = this.convertStepArgument(action.aggregateId);
                        const state = action.sagaState;
                        const uow = action.unitOfWork || 'unitOfWork';
                        stepBody += `            unitOfWorkService.registerSagaState(${aggId}, ${state}, ${uow});\n`;
                    }
                }

                // Generate step with dependencies
                let stepDependencies = '';
                if (dependencies.length > 0) {
                    const depVars = dependencies.map((dep: string) => stepMap.get(dep) || `${dep}Step`).join(', ');
                    stepDependencies = `, new ArrayList<>(Arrays.asList(${depVars}))`;
                }

                let stepCode = `        SagaSyncStep ${stepVar} = new SagaSyncStep("${stepName}", () -> {\n${stepBody}        }${stepDependencies});\n`;

                // Add compensation if present
                if (compensation && compensation.compensationActions) {
                    let compensationBody = '';
                    for (const compAction of compensation.compensationActions) {
                        if (compAction.$type === 'FuncCallServiceAction') {
                            const serviceRef = compAction.serviceRef;
                            const method = compAction.method;
                            const args = (compAction.args || []).map((arg: any) => this.convertStepArgument(arg)).join(', ');
                            compensationBody += `            this.${serviceRef}.${method}(${args});\n`;
                        } else if (compAction.$type === 'FuncRegisterSagaStateAction') {
                            const aggId = this.convertStepArgument(compAction.aggregateId);
                            const state = compAction.sagaState;
                            const uow = compAction.unitOfWork || 'unitOfWork';
                            compensationBody += `            unitOfWorkService.registerSagaState(${aggId}, ${state}, ${uow});\n`;
                        }
                    }
                    stepCode += `        ${stepVar}.registerCompensation(() -> {\n${compensationBody}        }, unitOfWork);\n`;
                }

                stepCode += `        workflow.addStep(${stepVar});\n`;
                stepsBody += stepCode + '\n';
            }

            // Add variable declarations to context if needed
            const variableDecls = Array.from(new Set(variableDeclarations)).join('\n    ');

            // Add necessary imports for ArrayList and Arrays if dependencies are used
            if (steps.some((s: any) => s.dependsOn?.dependencies?.length > 0)) {
                imports.push('import java.util.ArrayList;');
                imports.push('import java.util.Arrays;');
            }

            const context = {
                packageName,
                imports,
                className,
                constructorDependencies,
                buildParams,
                resultType,
                stepsBody,
                variableDeclarations: variableDecls
            };

            const template = this.loadTemplate('saga/functionality.hbs');
            const content = this.renderTemplate(template, context);
            outputs[className + '.java'] = content;
        }

        // Generate saga functionality classes for event processing workflows
        this.generateEventProcessingSagaFunctionalities(aggregate, options, outputs);

        return outputs;
    }

    private generateEventProcessingSagaFunctionalities(aggregate: any, options: { projectName: string }, outputs: Record<string, string>): void {
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

            const workflowName = sagaWorkflow.name;
            // Use event name for class name, not workflow name
            // e.g., "AnonymizeUserEvent" -> "AnonymizeUserAnswerFunctionalitySagas"
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
            // For event processing, constructor needs: service, sagaUnitOfWorkService, aggregateId, event fields, unitOfWork
            const workflowParams = sagaWorkflow.parameters || [];
            const constructorParams: Array<{ type: string; name: string }> = [
                { type: `${capitalizedAggregate}Service`, name: `${lowerAggregate}Service` },
                { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' },
                { type: 'Integer', name: 'aggregateId' } // Subscriber aggregate ID (e.g., answerId)
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

                // Map field names to match getter methods used in AnswerEventProcessing
                let paramName = fieldName;
                if (eventTypeName.includes('Anonymize') && fieldName === 'userAggregateId') {
                    paramName = 'studentAggregateId'; // Map userAggregateId to studentAggregateId for AnonymizeUserEvent
                } else if (eventTypeName.includes('UpdateStudentName') && fieldName === 'newName') {
                    paramName = 'updatedName'; // Map newName to updatedName
                } else if (eventTypeName.includes('UpdateStudentName') && fieldName === 'oldName') {
                    // Skip oldName - not used in constructor
                    return;
                }
                constructorParams.push({ type: fieldType, name: paramName });
            });

            // Add publisher version (standard Event field)
            constructorParams.push({ type: 'Integer', name: 'publisherAggregateVersion' });
            constructorParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });

            // Build buildWorkflow parameters by mapping workflow parameters to constructor parameters
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
                    // Workflow param like "answerId", "quizId", "executionId" -> map to "aggregateId" in constructor
                    constructorParamName = 'aggregateId';
                } else if (workflowParamName.includes('student') || workflowParamName.includes('user')) {
                    // Find matching constructor param
                    const matchingParam = constructorParams.find(p =>
                        p.name.toLowerCase().includes('student') ||
                        p.name.toLowerCase().includes('user') ||
                        (workflowParamName.includes('student') && p.name.toLowerCase() === 'studentaggregateid') ||
                        (workflowParamName.includes('user') && p.name.toLowerCase() === 'useraggregateid')
                    );
                    constructorParamName = matchingParam?.name || originalParamName;
                } else {
                    // Try to find exact match or similar
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

            // Generate workflow steps - uses DSL definitions if available, otherwise placeholder
            const stepsBody = this.generateEventProcessingWorkflowSteps(workflowName, aggregateName, lowerAggregate, workflowParams, sagaWorkflow);

            // Build constructor body - assign all params except unitOfWork (which is passed to buildWorkflow)
            const fieldsToStore = constructorParams.filter(p => p.name !== 'unitOfWork');
            const constructorBody = fieldsToStore.map(p => `        this.${p.name} = ${p.name};`).join('\n');
            const constructorBodyWithBuildWorkflow = constructorBody + `\n        this.buildWorkflow(${buildWorkflowCallParams.join(', ')});`;

            // Generate the class manually since we need to call buildWorkflow from constructor
            const buildParamsString = buildParams.map(p => `${p.type} ${p.name}`).join(', ');
            const constructorParamsString = constructorParams.map(p => `${p.type} ${p.name}`).join(', ');

            // Generate field declarations - only for stored fields (not unitOfWork)
            const fieldsString = fieldsToStore.map((p: { type: string; name: string }) => `    private ${p.type} ${p.name};`).join('\n');

            // Generate workflow field declarations (from DSL fields block)
            const workflowFieldsString = workflowFields.map((f: any) => {
                const fieldType = this.getParamTypeName(f.type, capitalizedAggregate);
                return `    private ${fieldType} ${f.name};`;
            }).join('\n');

            // Combine all fields
            const allFieldsString = [fieldsString, workflowFieldsString].filter(Boolean).join('\n');

            // Generate getters and setters for workflow fields
            const gettersSetters = workflowFields.map((f: any) => {
                const fieldType = this.getParamTypeName(f.type, capitalizedAggregate);
                const capitalizedName = this.capitalize(f.name);
                return `
    public void set${capitalizedName}(${fieldType} ${f.name}) {
        this.${f.name} = ${f.name};
    }

    public ${fieldType} get${capitalizedName}() {
        return ${f.name};
    }`;
            }).join('\n');

            const content = `package ${packageName};

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

            outputs[className + '.java'] = content;
        }
    }

    private generateWorkflowFunctionality(aggregate: any, workflow: any, options: { projectName: string }, packageName: string): string {
        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = this.capitalize(aggregate.name);
        const className = `${this.capitalize(workflow.name)}FunctionalitySagas`;
        const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name };

        const imports: string[] = [];
        imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaSyncStep;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${capitalizedAggregate}Service;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);

        // Check for step dependencies
        const workflowSteps = workflow.workflowSteps || [];
        const hasStepDependencies = workflowSteps.some((s: any) => s.dependsOn?.dependencies?.length > 0);
        if (hasStepDependencies) {
            imports.push('import java.util.ArrayList;');
            imports.push('import java.util.Arrays;');
        }

        // Check for saga state registration
        const usesRegisterState = workflowSteps.some((s: any) =>
            (s.stepActions || []).some((a: any) => a.$type === 'WorkflowRegisterStateAction') ||
            (s.compensation?.compensationActions || []).some((a: any) => a.$type === 'WorkflowRegisterStateAction')
        );
        if (usesRegisterState) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${capitalizedAggregate}SagaState;`);
            imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
        }

        // Build constructor parameters from workflow parameters
        const workflowParams = workflow.parameters || [];
        const constructorParams: Array<{ type: string; name: string }> = [
            { type: `${capitalizedAggregate}Service`, name: `${lowerAggregate}Service` },
            { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' }
        ];

        // Add workflow parameters (excluding UnitOfWork which goes last)
        const buildParams: Array<{ type: string; name: string }> = [];
        for (const param of workflowParams) {
            const paramName = param.name;
            const paramType = this.getParamTypeName(param.type, aggregate.name);

            if (paramType === 'SagaUnitOfWork' || paramType === 'UnitOfWork') {
                continue; // Skip unitOfWork, will add at end
            }

            constructorParams.push({ type: paramType, name: paramName });
            buildParams.push({ type: paramType, name: paramName });
        }

        // Add unitOfWork at the end
        constructorParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });
        buildParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });

        // Get workflow fields
        const workflowFields = workflow.workflowFields || [];

        // Build fields string - service dependencies
        const serviceFields = [
            `    private ${capitalizedAggregate}Service ${lowerAggregate}Service;`,
            `    private SagaUnitOfWorkService sagaUnitOfWorkService;`
        ];

        // Add workflow fields
        const extraFields = workflowFields.map((f: any) => {
            const fieldType = this.getParamTypeName(f.type, aggregate.name);
            return `    private ${fieldType} ${f.name};`;
        });

        const allFields = [...serviceFields, ...extraFields].join('\n');

        // Build constructor body
        const constructorAssignments = [
            `        this.${lowerAggregate}Service = ${lowerAggregate}Service;`,
            `        this.sagaUnitOfWorkService = sagaUnitOfWorkService;`
        ];

        // Build the buildWorkflow call arguments
        const buildWorkflowArgs = buildParams.map(p => p.name).join(', ');

        // Generate step body from DSL
        const stepsBody = this.generateWorkflowStepsFromDSL(workflowSteps, aggregate.name, lowerAggregate);

        // Generate getters and setters for workflow fields
        const gettersSetters = workflowFields.map((f: any) => {
            const fieldType = this.getParamTypeName(f.type, aggregate.name);
            const capitalizedName = this.capitalize(f.name);
            return `
    public void set${capitalizedName}(${fieldType} ${f.name}) {
        this.${f.name} = ${f.name};
    }

    public ${fieldType} get${capitalizedName}() {
        return ${f.name};
    }`;
        }).join('\n');

        const constructorParamsString = constructorParams.map(p => `${p.type} ${p.name}`).join(', ');
        const buildParamsString = buildParams.map(p => `${p.type} ${p.name}`).join(', ');

        return `package ${packageName};

${imports.join('\n')}

public class ${className} extends WorkflowFunctionality {
${allFields}

    public ${className}(${constructorParamsString}) {
${constructorAssignments.join('\n')}
        this.buildWorkflow(${buildWorkflowArgs});
    }

    public void buildWorkflow(${buildParamsString}) {
${stepsBody}
    }
${gettersSetters}
}`;
    }

    private findSagaWorkflowForEvent(aggregate: any, eventTypeName: string): any {
        // Ensure aggregate properties are initialized
        initializeAggregateProperties(aggregate);

        // Access workflows using the helper function - filter for saga workflows only
        const allWorkflows = getWorkflows(aggregate);

        const workflows = allWorkflows.filter((w: any) => {
            // Only include saga workflows (workflowType === 'saga')
            return w.workflowType === 'saga';
        });

        if (workflows.length === 0) {
            return null;
        }

        const eventNameWithoutEvent = eventTypeName.replace(/Event$/, '');
        // Normalize to all lowercase for case-insensitive comparison
        const lowerEventName = eventNameWithoutEvent.toLowerCase();

        // Try to find workflow by name pattern
        // Handle various naming patterns:
        // - "AnonymizeUserEvent" -> "anonymizeStudent" workflow (User -> Student)
        // - "InvalidateQuizEvent" -> "invalidateQuiz" workflow
        // - "DeleteExecutionEvent" -> "removeExecution" workflow (Delete -> Remove)
        // - "DisenrollStudentFromExecutionEvent" -> "unenrollStudentFromExecution" workflow (Disenroll -> Unenroll)
        // - "UpdateStudentNameEvent" -> "updateStudentName" workflow
        return workflows.find((w: any) => {
            const workflowName = (w.name || String(w)).toLowerCase();

            // Normalize the event name: delete -> remove, disenroll -> unenroll, user -> student
            const normalizedEventName = lowerEventName
                .replace('delete', 'remove')
                .replace('disenroll', 'unenroll');

            // Also try converting user to student and vice versa
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

    private generateEventProcessingWorkflowSteps(workflowName: string, aggregateName: string, lowerAggregate: string, workflowParams: any[], sagaWorkflow?: any): string {
        // Check if workflow has step definitions
        const workflowSteps = sagaWorkflow?.workflowSteps || [];

        if (workflowSteps.length > 0) {
            return this.generateWorkflowStepsFromDSL(workflowSteps, aggregateName, lowerAggregate);
        }

        // Generate a basic placeholder workflow structure
        return `        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);

        // TODO: Implement workflow steps for ${workflowName}
        // Example structure:
        // SagaSyncStep step1 = new SagaSyncStep("step1", () -> {
        //     // Step implementation
        // });
        // this.workflow.addStep(step1);`;
    }

    private generateWorkflowStepsFromDSL(workflowSteps: any[], aggregateName: string, lowerAggregate: string): string {
        const lines: string[] = [];
        lines.push('        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);');
        lines.push('');

        const stepVarNames: Map<string, string> = new Map();

        // First pass: create step variable names
        for (const step of workflowSteps) {
            const stepName = step.stepName;
            // Avoid double "Step" suffix
            const varName = stepName.endsWith('Step') ? stepName : `${stepName}Step`;
            stepVarNames.set(stepName, varName);
        }

        // Second pass: generate step code
        for (const step of workflowSteps) {
            const stepName = step.stepName;
            const stepVar = stepVarNames.get(stepName)!;
            const actions = step.stepActions || [];
            const dependencies = step.dependsOn?.dependencies || [];
            const compensation = step.compensation;

            // Generate step body actions
            let stepBody = '';
            for (const action of actions) {
                stepBody += this.generateWorkflowAction(action, aggregateName, lowerAggregate);
            }

            // Generate step with dependencies
            let stepDependencies = '';
            if (dependencies.length > 0) {
                const depVars = dependencies.map((dep: string) => stepVarNames.get(dep) || `${dep}Step`).join(', ');
                stepDependencies = `, new ArrayList<>(Arrays.asList(${depVars}))`;
            }

            lines.push(`        SagaSyncStep ${stepVar} = new SagaSyncStep("${stepName}", () -> {`);
            lines.push(stepBody);
            lines.push(`        }${stepDependencies});`);
            lines.push('');

            // Add compensation if present
            if (compensation && compensation.compensationActions && compensation.compensationActions.length > 0) {
                lines.push(`        ${stepVar}.registerCompensation(() -> {`);
                for (const compAction of compensation.compensationActions) {
                    lines.push(this.generateWorkflowAction(compAction, aggregateName, lowerAggregate));
                }
                lines.push('        }, unitOfWork);');
                lines.push('');
            }
        }

        // Add all steps to workflow
        for (const step of workflowSteps) {
            const stepVar = stepVarNames.get(step.stepName)!;
            lines.push(`        this.workflow.addStep(${stepVar});`);
        }

        return lines.join('\n');
    }

    private generateWorkflowAction(action: any, aggregateName: string, lowerAggregate: string): string {
        switch (action.$type) {
            case 'WorkflowCallAction': {
                const serviceRef = action.serviceRef;
                const method = action.method;
                const args = (action.args || []).map((arg: any) => this.convertWorkflowArg(arg)).join(', ');
                const callExpr = `${serviceRef}.${method}(${args})`;

                if (action.assignTo) {
                    return `            this.${action.assignTo} = ${callExpr};\n`;
                } else {
                    return `            ${callExpr};\n`;
                }
            }
            case 'WorkflowExtractAction': {
                const source = this.convertWorkflowArg(action.source);
                const target = action.target;

                if (action.filterField && action.filterValue) {
                    const filterField = action.filterField;
                    const filterValue = this.convertWorkflowArg(action.filterValue);
                    return `            this.${target} = ${source}.stream().filter(p -> p.get${this.capitalize(filterField)}().equals(${filterValue})).findFirst().orElse(null);\n`;
                } else {
                    return `            this.${target} = ${source};\n`;
                }
            }
            case 'WorkflowRegisterStateAction': {
                const aggregateId = this.convertWorkflowArg(action.aggregateId);
                const sagaState = action.sagaState;
                return `            sagaUnitOfWorkService.registerSagaState(${aggregateId}, ${sagaState}, unitOfWork);\n`;
            }
            case 'WorkflowSetFieldAction': {
                const fieldName = action.fieldName;
                const value = this.convertWorkflowArg(action.value);
                return `            this.${fieldName} = ${value};\n`;
            }
            default:
                return `            // Unknown action type: ${action.$type}\n`;
        }
    }

    private convertWorkflowArg(arg: any): string {
        if (!arg) return 'null';

        // WorkflowArg has ref and chain properties
        const ref = arg.ref || '';
        const chain = arg.chain || [];

        if (chain.length > 0) {
            // Build the chain: ref.chain[0].chain[1]...
            let result = ref;
            for (const part of chain) {
                // Use getter for each part
                result += `.get${this.capitalize(part)}()`;
            }
            return result;
        }

        return ref;
    }

    private getParamTypeName(paramType: any, aggregateName: string, useSagaDto: boolean = false): string {
        if (!paramType) return 'Object';

        // PrimitiveType
        if (paramType.typeName) {
            return paramType.typeName;
        }

        // EntityType (reference to entity)
        if (paramType.type?.ref?.name) {
            const entityName = paramType.type.ref.name;
            // Use SagaDto only if explicitly requested and for root entity
            if (useSagaDto && entityName === aggregateName) {
                return `Saga${aggregateName}Dto`;
            }
            return `${entityName}Dto`;
        }

        // ListType
        if (paramType.$type === 'ListType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `List<${elementType}>`;
        }

        // SetType
        if (paramType.$type === 'SetType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `Set<${elementType}>`;
        }

        // OptionalType
        if (paramType.$type === 'OptionalType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `Optional<${elementType}>`;
        }

        // BuiltinType
        if (paramType.name === 'UnitOfWork') {
            return 'SagaUnitOfWork';
        }
        if (paramType.name === 'AggregateState') {
            return 'AggregateState';
        }

        return 'Object';
    }

    private convertStepArgument(arg: any): string {
        if (typeof arg === 'string') {
            return arg;
        }
        if (arg.$type === 'PropertyChainExpression' || arg.$type === 'MethodCall') {
            // Convert expression to Java code
            return this.convertExpressionToJava(arg);
        }
        if (arg.value !== undefined) {
            return String(arg.value);
        }
        // Handle parameter references (objects with name property)
        if (arg.name) {
            return arg.name;
        }
        // Handle reference objects
        if (arg.$refText) {
            return arg.$refText;
        }
        // Handle ref objects (Langium references)
        if (arg.ref && arg.ref.name) {
            return arg.ref.name;
        }
        // Fallback - try to get a meaningful string
        if (typeof arg === 'object' && arg !== null) {
            return 'null /* TODO: fix argument */';
        }
        return String(arg);
    }

    private convertStepExpression(expr: any): string {
        if (typeof expr === 'string') {
            return expr;
        }
        if (expr.$type) {
            return this.convertExpressionToJava(expr);
        }
        return String(expr);
    }

    private convertExpressionToJava(expr: any): string {
        // Basic expression conversion - can be expanded
        if (expr.$type === 'PropertyChainExpression') {
            const head = expr.head?.name || '';
            let result = `this.${head}`;
            // Handle property access chains
            return result;
        }
        if (expr.$type === 'MethodCall') {
            const receiver = this.convertExpressionToJava(expr.receiver);
            const method = expr.method;
            const args = (expr.arguments || []).map((a: any) => this.convertStepArgument(a)).join(', ');
            return `${receiver}.${method}(${args})`;
        }
        return String(expr);
    }

    private inferVariableType(methodName: string, defaultType?: string): string {
        // Simple inference - can be improved
        if (methodName.includes('Dto') || methodName.includes('get')) {
            return defaultType || 'Object';
        }
        return defaultType || 'Object';
    }

    private inferVariableTypeFromExpression(expr: string): string {
        // Simple inference based on expression
        if (expr.includes('Dto')) {
            return 'Object'; // Will need proper type resolution
        }
        return 'Object';
    }

    private generateCrudSagaFunctionalities(aggregate: any, options: { projectName: string }, packageName: string): Record<string, string> {
        const outputs: Record<string, string> = {};
        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = this.capitalize(aggregate.name);
        const dtoType = `${capitalizedAggregate}Dto`;
        const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name };
        const entityName = rootEntity.name;

        const crudOperations: any[] = [
            {
                name: `create${capitalizedAggregate}`,
                stepName: `create${capitalizedAggregate}Step`,
                params: [{ type: dtoType, name: `${lowerAggregate}Dto` }],
                resultType: dtoType,
                resultField: `created${capitalizedAggregate}Dto`,
                resultSetter: `setCreated${capitalizedAggregate}Dto`,
                resultGetter: `getCreated${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.create${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}Dto`, 'unitOfWork']
            },
            {
                name: `get${capitalizedAggregate}ById`,
                stepName: `get${capitalizedAggregate}Step`,
                params: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
                resultType: dtoType,
                resultField: `${lowerAggregate}Dto`,
                resultSetter: `set${capitalizedAggregate}Dto`,
                resultGetter: `get${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.get${capitalizedAggregate}ById`,
                serviceArgs: [`${lowerAggregate}AggregateId`, 'unitOfWork']
            },
            {
                name: `update${capitalizedAggregate}`,
                stepName: `update${capitalizedAggregate}Step`,
                params: [
                    { type: 'Integer', name: `${lowerAggregate}AggregateId` },
                    { type: dtoType, name: `${lowerAggregate}Dto` }
                ],
                resultType: dtoType,
                resultField: `updated${capitalizedAggregate}Dto`,
                resultSetter: `setUpdated${capitalizedAggregate}Dto`,
                resultGetter: `getUpdated${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.update${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}AggregateId`, `${lowerAggregate}Dto`, 'unitOfWork']
            },
            {
                name: `delete${capitalizedAggregate}`,
                stepName: `delete${capitalizedAggregate}Step`,
                params: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
                // For delete, we also keep a reference to the asset being deleted
                resultType: dtoType,
                resultField: `deleted${capitalizedAggregate}Dto`,
                resultSetter: `setDeleted${capitalizedAggregate}Dto`,
                resultGetter: `getDeleted${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.delete${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}AggregateId`, 'unitOfWork']
            }
        ];

        const searchableProperties = this.getSearchableProperties(rootEntity);
        if (searchableProperties.length > 0) {
            const searchParams = searchableProperties.map(prop => ({
                type: prop.type,
                name: prop.name
            }));

            crudOperations.push({
                name: `search${capitalizedAggregate}s`,
                stepName: `search${capitalizedAggregate}sStep`,
                params: searchParams,
                resultType: `List<${dtoType}>`,
                resultField: `searched${entityName}Dtos`,
                resultSetter: `setSearched${entityName}Dtos`,
                resultGetter: `getSearched${entityName}Dtos`,
                serviceCall: `${lowerAggregate}Service.search${capitalizedAggregate}s`,
                serviceArgs: [...searchableProperties.map((p: any) => p.name), 'unitOfWork']
            });
        } else {
            crudOperations.push({
                name: `getAll${capitalizedAggregate}s`,
                stepName: `getAll${capitalizedAggregate}sStep`,
                params: [],
                resultType: `List<${dtoType}>`,
                resultField: `${lowerAggregate}s`,
                resultSetter: `set${capitalizedAggregate}s`,
                resultGetter: `get${capitalizedAggregate}s`,
                serviceCall: `${lowerAggregate}Service.getAll${capitalizedAggregate}s`,
                serviceArgs: ['unitOfWork']
            });
        }

        for (const op of crudOperations) {
            const className = `${this.capitalize(op.name)}FunctionalitySagas`;

            const imports: string[] = [];
            imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${capitalizedAggregate}Service;`);
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);
            imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
            imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
            imports.push(`import ${basePackage}.ms.sagas.workflow.SagaSyncStep;`);
            imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);

            const enumTypes = new Set<string>();
            const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'Double', 'Float', 'LocalDateTime', 'LocalDate', 'BigDecimal', 'void', 'UnitOfWork'];

            const addEnumTypeIfNeeded = (type: string | undefined) => {
                if (!type) return;
                const typeName = type.replace(/List<|Set<|>/g, '').trim();
                if (!typeName) return;

                if (
                    !primitiveTypes.includes(typeName) &&
                    !typeName.endsWith('Dto') &&
                    !typeName.includes('<') &&
                    typeName.charAt(0) === typeName.charAt(0).toUpperCase()
                ) {
                    enumTypes.add(typeName);
                }
            };

            if (op.params) {
                op.params.forEach((p: any) => addEnumTypeIfNeeded(p.type));
            }
            addEnumTypeIfNeeded(typeof op.resultType === 'string' ? op.resultType : undefined);

            enumTypes.forEach(enumType => {
                const enumImport = `${basePackage}.${options.projectName.toLowerCase()}.shared.enums.${enumType}`;
                imports.push(`import ${enumImport};`);
            });

            const isDeleteOperation = op.name.startsWith('delete');
            const isUpdateOperation = op.name.startsWith('update');
            const isGetAllOperation = op.name.startsWith('getAll');
            const isSearchOperation = op.name.startsWith('search');
            if (isDeleteOperation || isUpdateOperation) {
                imports.push('import java.util.ArrayList;');
                imports.push('import java.util.Arrays;');
                imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${capitalizedAggregate}SagaState;`);
                imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
            }
            if (isGetAllOperation || (op.resultType && op.resultType.includes('List<'))) {
                imports.push('import java.util.List;');
            }

            let fieldsDeclaration = '';

            const isGetByIdOperation = op.name === `get${capitalizedAggregate}ById`;
            const keepParamFields =
                op.name !== `create${capitalizedAggregate}` &&
                !isDeleteOperation &&
                !isGetByIdOperation &&
                !isUpdateOperation &&
                !isSearchOperation;
            if (keepParamFields) {
                for (const param of op.params) {
                    fieldsDeclaration += `    private ${param.type} ${param.name};\n`;
                }
            }

            if (op.resultField) {
                fieldsDeclaration += `    private ${op.resultType} ${op.resultField};\n`;
            }

            fieldsDeclaration += `    private final ${capitalizedAggregate}Service ${lowerAggregate}Service;\n`;
            fieldsDeclaration += `    private final SagaUnitOfWorkService unitOfWorkService;`;

            const constructorParams = [
                `${capitalizedAggregate}Service ${lowerAggregate}Service`,
                'SagaUnitOfWorkService unitOfWorkService',
                ...op.params.map((p: any) => `${p.type} ${p.name}`),
                'SagaUnitOfWork unitOfWork'
            ];

            const buildWorkflowParams = [
                ...op.params.map((p: any) => `${p.type} ${p.name}`),
                'SagaUnitOfWork unitOfWork'
            ];

            const buildWorkflowCallArgs = [
                ...op.params.map((p: any) => p.name),
                'unitOfWork'
            ];

            let workflowBody = '';
            const idParamName = op.params[0]?.name || `${lowerAggregate}AggregateId`;

            if (isDeleteOperation) {
                const readMethod = `${lowerAggregate}Service.get${capitalizedAggregate}ById`;
                const readStateConst = `${capitalizedAggregate}SagaState.READ_${capitalizedAggregate.toUpperCase()}`;

                workflowBody = `
        SagaSyncStep get${capitalizedAggregate}Step = new SagaSyncStep("get${capitalizedAggregate}Step", () -> {
            ${op.resultType} ${op.resultField} = ${readMethod}(${idParamName}, unitOfWork);
            ${op.resultSetter}(${op.resultField});
            unitOfWorkService.registerSagaState(${idParamName}, ${readStateConst}, unitOfWork);
        });

        get${capitalizedAggregate}Step.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(${idParamName}, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep delete${capitalizedAggregate}Step = new SagaSyncStep("delete${capitalizedAggregate}Step", () -> {
            ${op.serviceCall}(${op.serviceArgs.join(', ')});
        }, new ArrayList<>(Arrays.asList(get${capitalizedAggregate}Step)));

        workflow.addStep(get${capitalizedAggregate}Step);
        workflow.addStep(delete${capitalizedAggregate}Step);`;
            } else if (isUpdateOperation) {
                const readStateConst = `${capitalizedAggregate}SagaState.READ_${capitalizedAggregate.toUpperCase()}`;
                const dtoParamName = op.params[1]?.name || `${lowerAggregate}Dto`;

                workflowBody = `
        SagaSyncStep get${capitalizedAggregate}Step = new SagaSyncStep("get${capitalizedAggregate}Step", () -> {
            unitOfWorkService.registerSagaState(${idParamName}, ${readStateConst}, unitOfWork);
        });

        get${capitalizedAggregate}Step.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(${idParamName}, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep update${capitalizedAggregate}Step = new SagaSyncStep("update${capitalizedAggregate}Step", () -> {
            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${idParamName}, ${dtoParamName}, unitOfWork);
            ${op.resultSetter}(${op.resultField});
        }, new ArrayList<>(Arrays.asList(get${capitalizedAggregate}Step)));

        workflow.addStep(get${capitalizedAggregate}Step);
        workflow.addStep(update${capitalizedAggregate}Step);`;
            } else {
                let stepBody = '';
                if (op.resultType) {
                    stepBody = `            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${op.serviceArgs.join(', ')});
            ${op.resultSetter}(${op.resultField});`;
                } else {
                    stepBody = `            ${op.serviceCall}(${op.serviceArgs.join(', ')});`;
                }

                workflowBody = `
        SagaSyncStep ${op.stepName} = new SagaSyncStep("${op.stepName}", () -> {
${stepBody}
        });

        workflow.addStep(${op.stepName});`;
            }

            let gettersSettersCode = '';
            if (keepParamFields) {
                for (const param of op.params) {
                    const capitalizedParam = this.capitalize(param.name);
                    gettersSettersCode += `
    public ${param.type} get${capitalizedParam}() {
        return ${param.name};
    }

    public void set${capitalizedParam}(${param.type} ${param.name}) {
        this.${param.name} = ${param.name};
    }
`;
                }
            }

            if (op.resultField && op.resultGetter && op.resultSetter) {
                gettersSettersCode += `
    public ${op.resultType} ${op.resultGetter}() {
        return ${op.resultField};
    }

    public void ${op.resultSetter}(${op.resultType} ${op.resultField}) {
        this.${op.resultField} = ${op.resultField};
    }`;
            }

            // Generate the class content
            const content = `package ${packageName};

${imports.join('\n')}

public class ${className} extends WorkflowFunctionality {
${fieldsDeclaration}

    public ${className}(${constructorParams.join(', ')}) {
        this.${lowerAggregate}Service = ${lowerAggregate}Service;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(${buildWorkflowCallArgs.join(', ')});
    }

    public void buildWorkflow(${buildWorkflowParams.join(', ')}) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
${workflowBody}
    }
${gettersSettersCode}
}
`;

            outputs[className + '.java'] = content;
        }

        return outputs;
    }

    private getSearchableProperties(entity: any): { name: string; type: string }[] {
        if (!entity.properties) return [];

        const searchableTypes = ['String', 'Boolean'];
        const properties: { name: string; type: string }[] = [];

        for (const prop of entity.properties) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            let isEnum = false;
            if (propType && typeof propType === 'object' && propType.$type === 'EntityType' && propType.type) {
                const ref = propType.type.ref;
                if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                    isEnum = true;
                } else if (propType.type.$refText) {
                    const javaType = this.resolveJavaType(prop.type);
                    if (!this.isPrimitiveType(javaType) && !this.isEntityType(javaType) &&
                        !javaType.startsWith('List<') && !javaType.startsWith('Set<')) {
                        isEnum = true;
                    }
                }
            }

            if (searchableTypes.includes(typeName) || isEnum) {
                const javaType = this.resolveJavaType(prop.type);
                properties.push({
                    name: prop.name,
                    type: javaType
                });
            }
        }

        for (const prop of entity.properties) {
            const typeNode: any = (prop as any).type;
            if (!typeNode || typeNode.$type !== 'EntityType' || !typeNode.type) continue;

            const refEntity = typeNode.type.ref as any;
            if (!refEntity || !refEntity.properties) continue;

            for (const relProp of refEntity.properties as any[]) {
                if (!relProp.name || !relProp.name.endsWith('AggregateId')) continue;

                const relType = relProp.type;
                const relTypeName = relType?.typeName || relType?.type?.$refText || relType?.$refText || '';
                if (relTypeName !== 'Integer' && relTypeName !== 'Long') continue;

                properties.push({
                    name: relProp.name,
                    type: relTypeName
                });
            }
        }

        return properties;
    }

}