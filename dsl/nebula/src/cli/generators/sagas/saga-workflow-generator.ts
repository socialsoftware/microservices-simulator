import { SagaHelpers } from './saga-helpers.js';
import { SagaGenerationOptions } from './saga-generator.js';
import { StringUtils } from '../../utils/string-utils.js';



export class SagaWorkflowGenerator {
    private getBasePackage(options: SagaGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in SagaGenerationOptions');
        }
        return options.basePackage;
    }
    private helpers = new SagaHelpers();

    

    generateWorkflowFunctionality(aggregate: any, workflow: any, options: SagaGenerationOptions, packageName: string): string {
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const className = `${StringUtils.capitalize(workflow.name)}FunctionalitySagas`;
        const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name };

        const imports: string[] = [];
        imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.push(`import ${basePackage}.ms.coordination.workflow.command.CommandGateway;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaStep;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.ServiceMapping;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.command.${lowerAggregate}.*;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);

        
        const workflowSteps = workflow.workflowSteps || [];
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
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate.sagas.states.${capitalizedAggregate}SagaState;`);
            imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
        }

        
        const workflowParams = workflow.parameters || [];
        const constructorParams: Array<{ type: string; name: string }> = [
            { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' },
            { type: 'CommandGateway', name: 'commandGateway' }
        ];

        
        const buildParams: Array<{ type: string; name: string }> = [];
        for (const param of workflowParams) {
            const paramName = param.name;
            const paramType = this.helpers.getParamTypeName(param.type, aggregate.name);

            if (paramType === 'SagaUnitOfWork' || paramType === 'UnitOfWork') {
                continue; 
            }

            constructorParams.push({ type: paramType, name: paramName });
            buildParams.push({ type: paramType, name: paramName });
        }

        
        constructorParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });
        buildParams.push({ type: 'SagaUnitOfWork', name: 'unitOfWork' });

        
        const workflowFields = workflow.workflowFields || [];

        
        const serviceFields = [
            `    private SagaUnitOfWorkService sagaUnitOfWorkService;`,
            `    private CommandGateway commandGateway;`
        ];

        
        const extraFields = workflowFields.map((f: any) => {
            const fieldType = this.helpers.getParamTypeName(f.type, aggregate.name);
            return `    private ${fieldType} ${f.name};`;
        });

        const allFields = [...serviceFields, ...extraFields].join('\n');

        
        const constructorAssignments = [
            `        this.sagaUnitOfWorkService = sagaUnitOfWorkService;`,
            `        this.commandGateway = commandGateway;`
        ];

        
        const buildWorkflowArgs = buildParams.map(p => p.name).join(', ');

        
        const stepsBody = this.generateWorkflowStepsFromDSL(workflowSteps, aggregate.name, lowerAggregate);

        
        const gettersSetters = workflowFields.map((f: any) => {
            const fieldType = this.helpers.getParamTypeName(f.type, aggregate.name);
            const capitalizedName = StringUtils.capitalize(f.name);
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

    

    generateEventProcessingWorkflowSteps(workflowName: string, aggregateName: string, lowerAggregate: string, workflowParams: any[], sagaWorkflow?: any): string {
        
        const workflowSteps = sagaWorkflow?.workflowSteps || [];

        if (workflowSteps.length > 0) {
            return this.generateWorkflowStepsFromDSL(workflowSteps, aggregateName, lowerAggregate);
        }

        
        return `        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);

        // No workflow steps defined in DSL - implement via workflowSteps block
        // Example structure:
        // SagaStep step1 = new SagaStep("step1", () -> {
        //     // Step implementation
        // });
        // this.workflow.addStep(step1);`;
    }

    

    generateWorkflowStepsFromDSL(workflowSteps: any[], aggregateName: string, lowerAggregate: string): string {
        const lines: string[] = [];
        lines.push('        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);');
        lines.push('');

        const stepVarNames: Map<string, string> = new Map();

        
        for (const step of workflowSteps) {
            const stepName = step.stepName;
            
            const varName = stepName.endsWith('Step') ? stepName : `${stepName}Step`;
            stepVarNames.set(stepName, varName);
        }

        
        for (const step of workflowSteps) {
            const stepName = step.stepName;
            const stepVar = stepVarNames.get(stepName)!;
            const actions = step.stepActions || [];
            const dependencies = step.dependsOn?.dependencies || [];
            const compensation = step.compensation;

            
            let stepBody = '';
            for (const action of actions) {
                stepBody += this.generateWorkflowAction(action, aggregateName, lowerAggregate);
            }

            
            let stepDependencies = '';
            if (dependencies.length > 0) {
                const depVars = dependencies.map((dep: string) => stepVarNames.get(dep) || `${dep}Step`).join(', ');
                stepDependencies = `, new ArrayList<>(Arrays.asList(${depVars}))`;
            }

            lines.push(`        SagaStep ${stepVar} = new SagaStep("${stepName}", () -> {`);
            lines.push(stepBody);
            lines.push(`        }${stepDependencies});`);
            lines.push('');

            
            if (compensation && compensation.compensationActions && compensation.compensationActions.length > 0) {
                lines.push(`        ${stepVar}.registerCompensation(() -> {`);
                for (const compAction of compensation.compensationActions) {
                    lines.push(this.generateWorkflowAction(compAction, aggregateName, lowerAggregate));
                }
                lines.push('        }, unitOfWork);');
                lines.push('');
            }
        }

        
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
                    return `            this.${target} = ${source}.stream().filter(p -> p.get${StringUtils.capitalize(filterField)}().equals(${filterValue})).findFirst().orElse(null);\n`;
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

