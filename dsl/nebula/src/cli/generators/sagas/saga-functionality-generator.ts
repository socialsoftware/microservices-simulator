import { OrchestrationBase } from '../common/orchestration-base.js';
import { initializeAggregateProperties, getWorkflows } from '../../utils/aggregate-helpers.js';

export class SagaFunctionalityGenerator extends OrchestrationBase {
    generateForAggregate(aggregate: any, options: { projectName: string }): Record<string, string> {
        const outputs: Record<string, string> = {};
        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const packageName = `${basePackage}.${options.projectName.toLowerCase()}.sagas.coordination.${lowerAggregate}`;

        const endpoints = aggregate.webApiEndpoints?.endpoints || [];
        for (const endpoint of endpoints) {
            const methodName: string = endpoint.methodName;
            if (!methodName) continue;

            const className = `${this.capitalize(methodName)}FunctionalitySagas`;
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

            // Generate basic workflow steps (placeholder - will need manual implementation)
            const stepsBody = this.generateEventProcessingWorkflowSteps(workflowName, aggregateName, lowerAggregate, workflowParams);

            // Build constructor body - assign all params except unitOfWork (which is passed to buildWorkflow)
            const fieldsToStore = constructorParams.filter(p => p.name !== 'unitOfWork');
            const constructorBody = fieldsToStore.map(p => `        this.${p.name} = ${p.name};`).join('\n');
            const constructorBodyWithBuildWorkflow = constructorBody + `\n        this.buildWorkflow(${buildWorkflowCallParams.join(', ')});`;

            // Generate the class manually since we need to call buildWorkflow from constructor
            const buildParamsString = buildParams.map(p => `${p.type} ${p.name}`).join(', ');
            const constructorParamsString = constructorParams.map(p => `${p.type} ${p.name}`).join(', ');

            // Generate field declarations - only for stored fields (not unitOfWork)
            const fieldsString = fieldsToStore.map((p: { type: string; name: string }) => `    private ${p.type} ${p.name};`).join('\n');

            const content = `package ${packageName};

${imports.map(i => i).join('\n')}

public class ${className} extends WorkflowFunctionality {
${fieldsString}

    public ${className}(${constructorParamsString}) {
${constructorBodyWithBuildWorkflow}
    }

    public void buildWorkflow(${buildParamsString}) {
${stepsBody}
    }
}`;

            outputs[className + '.java'] = content;
        }
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

    private generateEventProcessingWorkflowSteps(workflowName: string, aggregateName: string, lowerAggregate: string, workflowParams: any[]): string {
        // Generate a basic placeholder workflow structure
        // This will need to be manually implemented based on the actual workflow requirements
        return `        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);

        // TODO: Implement workflow steps for ${workflowName}
        // Example structure:
        // SagaSyncStep step1 = new SagaSyncStep("step1", () -> {
        //     // Step implementation
        // });
        // this.workflow.addStep(step1);`;
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

}