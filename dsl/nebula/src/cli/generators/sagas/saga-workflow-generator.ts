import { OrchestrationBase } from '../common/orchestration-base.js';
import { SagaHelpers } from './saga-helpers.js';

/**
 * Generates saga workflow functionalities from DSL definitions
 */
export class SagaWorkflowGenerator extends OrchestrationBase {
    private helpers = new SagaHelpers();

    /**
     * Generate a workflow functionality class from a workflow definition
     */
    generateWorkflowFunctionality(aggregate: any, workflow: any, options: { projectName: string }, packageName: string): string {
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
            const paramType = this.helpers.getParamTypeName(param.type, aggregate.name);

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
            const fieldType = this.helpers.getParamTypeName(f.type, aggregate.name);
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
            const fieldType = this.helpers.getParamTypeName(f.type, aggregate.name);
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

    /**
     * Generate workflow steps for event processing
     */
    generateEventProcessingWorkflowSteps(workflowName: string, aggregateName: string, lowerAggregate: string, workflowParams: any[], sagaWorkflow?: any): string {
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

    /**
     * Generate workflow steps from DSL definitions
     */
    generateWorkflowStepsFromDSL(workflowSteps: any[], aggregateName: string, lowerAggregate: string): string {
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

    /**
     * Generate code for a single workflow action
     */
    private generateWorkflowAction(action: any, aggregateName: string, lowerAggregate: string): string {
        switch (action.$type) {
            case 'WorkflowCallAction': {
                const serviceRef = action.serviceRef;
                const method = action.method;
                const args = (action.args || []).map((arg: any) => this.helpers.convertWorkflowArg(arg)).join(', ');
                const callExpr = `${serviceRef}.${method}(${args})`;

                if (action.assignTo) {
                    return `            this.${action.assignTo} = ${callExpr};\n`;
                } else {
                    return `            ${callExpr};\n`;
                }
            }
            case 'WorkflowExtractAction': {
                const source = this.helpers.convertWorkflowArg(action.source);
                const target = action.target;

                if (action.filterField && action.filterValue) {
                    const filterField = action.filterField;
                    const filterValue = this.helpers.convertWorkflowArg(action.filterValue);
                    return `            this.${target} = ${source}.stream().filter(p -> p.get${this.capitalize(filterField)}().equals(${filterValue})).findFirst().orElse(null);\n`;
                } else {
                    return `            this.${target} = ${source};\n`;
                }
            }
            case 'WorkflowRegisterStateAction': {
                const aggregateId = this.helpers.convertWorkflowArg(action.aggregateId);
                const sagaState = action.sagaState;
                return `            sagaUnitOfWorkService.registerSagaState(${aggregateId}, ${sagaState}, unitOfWork);\n`;
            }
            case 'WorkflowSetFieldAction': {
                const fieldName = action.fieldName;
                const value = this.helpers.convertWorkflowArg(action.value);
                return `            this.${fieldName} = ${value};\n`;
            }
            default:
                return `            // Unknown action type: ${action.$type}\n`;
        }
    }
}

