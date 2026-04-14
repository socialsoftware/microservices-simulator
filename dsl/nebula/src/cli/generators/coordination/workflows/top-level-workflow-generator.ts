import { Aggregate, Method, Model, TopLevelWorkflow, TopLevelWorkflowStep, WorkflowActionCall, WorkflowCallArg } from "../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../common/unified-type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import { getMethods } from "../../../utils/aggregate-helpers.js";

export interface TopLevelWorkflowGenerationOptions {
    projectName: string;
    basePackage: string;
    models?: Model[];
}

interface SagaStateRef {
    enumClass: string;
    values: string[];
}

export class TopLevelWorkflowGenerator {

    generateAll(workflow: TopLevelWorkflow, aggregates: Aggregate[], options: TopLevelWorkflowGenerationOptions): Array<{ fileName: string; content: string }> {
        const orchestrator = this.generate(workflow, aggregates, options);
        const requestDto = this.generateRequestDto(workflow, options);
        const controller = this.generateController(workflow, options);
        return [orchestrator, requestDto, controller];
    }

    generate(workflow: TopLevelWorkflow, aggregates: Aggregate[], options: TopLevelWorkflowGenerationOptions): { fileName: string; content: string } {
        const workflowName = workflow.name;
        const className = `${workflowName}Workflow`;
        const packageName = getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'workflows');

        const steps = workflow.workflowSteps || [];

        const targetedAggregates = new Set<string>();
        for (const step of steps) {
            if (step.action?.target) targetedAggregates.add(step.action.target);
            if (step.compensate?.target) targetedAggregates.add(step.compensate.target);
        }

        const stepResultTypes = new Map<string, { javaType: string; aggregateName?: string }>();
        for (const step of steps) {
            const t = this.resolveReturnType(step.action, aggregates);
            if (t) stepResultTypes.set(step.name, t);
        }

        const sagaStateIndex = this.buildSagaStateIndex(options.models || []);

        const imports = this.buildImports(aggregates, targetedAggregates, steps, stepResultTypes, options, sagaStateIndex);
        const serviceFields = this.buildServiceFields(targetedAggregates);
        const stepResultFields = this.buildStepResultFields(stepResultTypes);
        const executeMethod = this.buildExecuteMethod(workflow, aggregates, stepResultTypes, sagaStateIndex);

        const content = `package ${packageName};

${imports.join('\n')}

@Component
public class ${className} extends WorkflowFunctionality {

    @Autowired private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired private CommandGateway commandGateway;
${serviceFields}
${stepResultFields}
${executeMethod}
}
`;

        return { fileName: `${className}.java`, content };
    }

    private buildSagaStateIndex(models: Model[]): Map<string, SagaStateRef> {
        const index = new Map<string, SagaStateRef>();
        for (const model of models) {
            const blocks = (model as any).sagaStatesBlocks || [];
            for (const block of blocks) {
                const groups = (block as any).groups || [];
                for (const group of groups) {
                    const values: string[] = [...(group.states || [])];
                    for (const value of values) {
                        index.set(value, { enumClass: group.name, values });
                    }
                }
            }
        }
        return index;
    }

    private buildImports(aggregates: Aggregate[], targeted: Set<string>, steps: TopLevelWorkflowStep[], stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>, options: TopLevelWorkflowGenerationOptions, sagaStateIndex: Map<string, SagaStateRef>): string[] {
        const imports = new Set<string>([
            `import org.springframework.beans.factory.annotation.Autowired;`,
            `import org.springframework.stereotype.Component;`,
            `import ${options.basePackage}.ms.coordination.workflow.WorkflowFunctionality;`,
            `import ${options.basePackage}.ms.coordination.workflow.command.CommandGateway;`,
            `import ${options.basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`,
            `import ${options.basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`,
            `import ${options.basePackage}.ms.sagas.workflow.SagaStep;`,
            `import ${options.basePackage}.ms.sagas.workflow.SagaWorkflow;`,
        ]);

        const config = getGlobalConfig();

        for (const aggName of targeted) {
            const lower = aggName.toLowerCase();
            imports.add(`import ${config.buildPackageName(options.projectName, 'microservices', lower, 'service')}.${aggName}Service;`);
        }

        for (const t of stepResultTypes.values()) {
            if (t.javaType.endsWith('Dto')) {
                imports.add(`import ${config.buildPackageName(options.projectName, 'shared', 'dtos')}.${t.javaType};`);
            }
        }

        for (const step of steps) {
            const target = step.action?.target;
            if (!target) continue;
            const targetMethod = this.findMethod(aggregates, target, step.action.method);
            if (!targetMethod) continue;
            const params = targetMethod.parameters || [];
            const callArgs = step.action.args || [];
            for (let i = 0; i < callArgs.length && i < params.length; i++) {
                const projection = this.findProjectionWrapper(target, callArgs[i], stepResultTypes, aggregates, params[i]);
                if (projection) {
                    const lower = target.toLowerCase();
                    imports.add(`import ${config.buildPackageName(options.projectName, 'microservices', lower, 'aggregate')}.${projection};`);
                }
            }
        }

        const usedEnumClasses = new Set<string>();
        for (const step of steps) {
            if (step.lockState) {
                const ref = sagaStateIndex.get(step.lockState);
                if (ref) usedEnumClasses.add(ref.enumClass);
            }
        }
        if (usedEnumClasses.size > 0) {
            imports.add(`import ${options.basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;`);
            for (const enumClass of usedEnumClasses) {
                imports.add(`import ${config.buildPackageName(options.projectName, 'shared', 'sagaStates')}.${enumClass};`);
            }
        }

        return Array.from(imports).sort();
    }

    private buildServiceFields(targeted: Set<string>): string {
        return Array.from(targeted).sort().map(agg => {
            const fieldName = agg.charAt(0).toLowerCase() + agg.slice(1) + 'Service';
            return `    @Autowired private ${agg}Service ${fieldName};`;
        }).join('\n');
    }

    private buildStepResultFields(stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>): string {
        const lines: string[] = [];
        for (const [stepName, t] of stepResultTypes) {
            if (!t.javaType || t.javaType === 'void') continue;
            lines.push(`    private ${t.javaType} ${stepName};`);
        }
        return lines.length > 0 ? '\n' + lines.join('\n') + '\n' : '';
    }

    private buildExecuteMethod(workflow: TopLevelWorkflow, aggregates: Aggregate[], stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>, sagaStateIndex: Map<string, SagaStateRef>): string {
        const inputs = workflow.inputs || [];
        const inputParams = inputs.map(i => `${this.javaTypeOfParamType(i.type)} ${i.name}`).join(', ');

        const lines: string[] = [];
        lines.push('');
        lines.push(`    public void execute(${inputParams}) {`);
        lines.push(`        SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("${workflow.name}");`);
        lines.push(`        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);`);
        lines.push('');

        let prevStepVar: string | undefined;
        const steps = workflow.workflowSteps || [];
        const hasMultipleSteps = steps.length > 1;
        if (hasMultipleSteps) {
            lines.push(`        java.util.ArrayList<pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep> prevDeps;`);
        }
        for (const step of steps) {
            const stepVar = `${step.name}Step`;
            const actionCall = this.renderActionCall(step.action, aggregates, stepResultTypes, step.name);
            const lockLines = this.renderLockGuards(step, stepResultTypes, aggregates, sagaStateIndex);
            lines.push(`        SagaStep ${stepVar} = new SagaStep("${step.name}", () -> {`);
            for (const lockLine of lockLines) {
                lines.push(`            ${lockLine}`);
            }
            lines.push(`            ${actionCall}`);
            lines.push(`        });`);

            if (prevStepVar) {
                lines.push(`        prevDeps = new java.util.ArrayList<>();`);
                lines.push(`        prevDeps.add(${prevStepVar});`);
                lines.push(`        ${stepVar}.setDependencies(prevDeps);`);
            }

            if (step.compensate) {
                const compCall = this.renderActionCall(step.compensate, aggregates, stepResultTypes);
                lines.push(`        ${stepVar}.registerCompensation(() -> {`);
                lines.push(`            ${compCall}`);
                lines.push(`        }, unitOfWork);`);
            }

            lines.push(`        workflow.addStep(${stepVar});`);
            lines.push('');
            prevStepVar = stepVar;
        }

        lines.push(`        this.executeWorkflow(unitOfWork);`);
        lines.push(`    }`);
        return lines.join('\n');
    }

    private renderLockGuards(step: TopLevelWorkflowStep, stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>, aggregates: Aggregate[], sagaStateIndex: Map<string, SagaStateRef>): string[] {
        if (!step.lockState || !step.lockId || !step.lockAggregate) return [];

        const ref = sagaStateIndex.get(step.lockState);
        if (!ref) {
            throw new Error(`Step '${step.name}': saga state '${step.lockState}' is not declared in any 'SagaStates' block`);
        }
        for (const f of step.forbidden || []) {
            if (!ref.values.includes(f)) {
                throw new Error(`Step '${step.name}': forbidden saga state '${f}' is not in '${ref.enumClass}'`);
            }
        }

        const idExpr = this.renderLockIdExpr(step.lockId, stepResultTypes);

        const forbiddenValues = (step.forbidden && step.forbidden.length > 0)
            ? [...step.forbidden]
            : [...ref.values];

        const lines: string[] = [];
        if (forbiddenValues.length > 0) {
            const forbiddenExpr = forbiddenValues
                .map(v => `${ref.enumClass}.${v}`)
                .join(', ');
            lines.push(`sagaUnitOfWorkService.verifySagaState(${idExpr}, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(${forbiddenExpr})));`);
        }
        lines.push(`sagaUnitOfWorkService.registerSagaState(${idExpr}, ${ref.enumClass}.${step.lockState}, unitOfWork);`);
        return lines;
    }

    private renderLockIdExpr(lockId: WorkflowCallArg, stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>): string {
        const head = lockId.name;
        const chain = lockId.chain || [];
        const isStepRef = stepResultTypes.has(head);

        let base = isStepRef ? `this.${head}` : head;
        for (const c of chain) {
            const cap = c.charAt(0).toUpperCase() + c.slice(1);
            base = `${base}.get${cap}()`;
        }
        return base;
    }

    private renderActionCall(action: WorkflowActionCall, aggregates: Aggregate[], stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>, assignTo?: string): string {
        const target = action.target;
        const method = action.method;
        const serviceField = target.charAt(0).toLowerCase() + target.slice(1) + 'Service';

        const methodNode = this.findMethod(aggregates, target, method);
        const params = methodNode?.parameters || [];

        const renderedArgs: string[] = [];
        const dslArgs = action.args || [];
        for (let i = 0; i < dslArgs.length; i++) {
            const arg = dslArgs[i];
            const expectedParam = params[i];
            renderedArgs.push(this.renderArg(arg, target, expectedParam, stepResultTypes, aggregates));
        }
        renderedArgs.push('unitOfWork');

        const callExpr = `${serviceField}.${method}(${renderedArgs.join(', ')})`;
        const returnType = stepResultTypes.get(assignTo || '')?.javaType;
        if (assignTo && returnType && returnType !== 'void') {
            return `this.${assignTo} = ${callExpr};`;
        }
        return `${callExpr};`;
    }

    private renderArg(arg: WorkflowCallArg, targetAggregate: string, expectedParam: any, stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>, aggregates: Aggregate[]): string {
        const head = arg.name;
        const chain = arg.chain || [];

        const isStepRef = stepResultTypes.has(head);

        let base: string;
        if (isStepRef) {
            base = `this.${head}`;
            for (const c of chain) {
                const cap = c.charAt(0).toUpperCase() + c.slice(1);
                base = `${base}.get${cap}()`;
            }
        } else {
            base = head;
            for (const c of chain) {
                const cap = c.charAt(0).toUpperCase() + c.slice(1);
                base = `${base}.get${cap}()`;
            }
        }

        if (isStepRef && chain.length === 0 && expectedParam) {
            const expectedType = this.javaTypeOfParamType(expectedParam.type);
            const stepType = stepResultTypes.get(head)?.javaType;
            if (expectedType && stepType && expectedType !== stepType) {
                const projectionWrapper = this.findProjectionWrapper(targetAggregate, arg, stepResultTypes, aggregates, expectedParam);
                if (projectionWrapper) {
                    return `new ${projectionWrapper}(${base})`;
                }
            }
        }

        return base;
    }

    private findProjectionWrapper(targetAggregate: string, arg: WorkflowCallArg, stepResultTypes: Map<string, { javaType: string; aggregateName?: string }>, aggregates: Aggregate[], expectedParam: any): string | undefined {
        if (!expectedParam || (arg.chain || []).length > 0) return undefined;
        const stepType = stepResultTypes.get(arg.name);
        if (!stepType?.aggregateName) return undefined;
        const expectedType = this.javaTypeOfParamType(expectedParam.type);
        if (!expectedType) return undefined;

        const targetAgg = aggregates.find(a => a.name === targetAggregate);
        if (!targetAgg) return undefined;
        const matches = (targetAgg.aggregateElements || []).some((el: any) =>
            el?.$type === 'Entity' && el.name === expectedType && el.aggregateRef === stepType.aggregateName
        );
        return matches ? expectedType : undefined;
    }

    private resolveReturnType(call: WorkflowActionCall, aggregates: Aggregate[]): { javaType: string; aggregateName?: string } | undefined {
        const targetAgg = call.target;
        const methodNode = this.findMethod(aggregates, targetAgg, call.method);
        if (methodNode?.returnType) {
            const rt = (methodNode.returnType as any);
            if (typeof rt !== 'string' || rt !== 'void') {
                const javaType = this.javaTypeOfParamType(rt);
                if (javaType) return { javaType, aggregateName: javaType.endsWith('Dto') ? javaType.slice(0, -3) : undefined };
            }
        }
        if (methodNode && (methodNode as any).actionBody) {
            const hasCreate = ((methodNode as any).actionBody.statements || []).some((s: any) => s?.$type === 'CreateActionStatement');
            if (hasCreate) return { javaType: `${targetAgg}Dto`, aggregateName: targetAgg };
        }
        const cap = targetAgg.charAt(0).toUpperCase() + targetAgg.slice(1);
        if (call.method === `get${cap}ById` || call.method === `create${cap}` || call.method === `update${cap}`) {
            return { javaType: `${targetAgg}Dto`, aggregateName: targetAgg };
        }
        if (call.method.startsWith('getAll')) {
            return { javaType: `java.util.List<${targetAgg}Dto>` };
        }
        return undefined;
    }

    private findMethod(aggregates: Aggregate[], aggregateName: string, methodName: string): Method | undefined {
        const agg = aggregates.find(a => a.name === aggregateName);
        if (!agg) return undefined;
        const methods = getMethods(agg);
        return methods.find(m => m.name === methodName);
    }

    private javaTypeOfParamType(type: any): string {
        if (!type) return '';
        if (typeof type === 'string') return type;
        return TypeResolver.resolveJavaType(type);
    }

    private generateRequestDto(workflow: TopLevelWorkflow, options: TopLevelWorkflowGenerationOptions): { fileName: string; content: string } {
        const packageName = getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'workflows');
        const className = `${workflow.name}WorkflowRequestDto`;
        const inputs = workflow.inputs || [];

        const fields = inputs.map(i => `    private ${this.javaTypeOfParamType(i.type)} ${i.name};`).join('\n');
        const gettersSetters = inputs.map(i => {
            const javaType = this.javaTypeOfParamType(i.type);
            const cap = i.name.charAt(0).toUpperCase() + i.name.slice(1);
            return `    public ${javaType} get${cap}() { return ${i.name}; }
    public void set${cap}(${javaType} ${i.name}) { this.${i.name} = ${i.name}; }`;
        }).join('\n');

        const content = `package ${packageName};

public class ${className} {
${fields}

    public ${className}() {}

${gettersSetters}
}
`;
        return { fileName: `${className}.java`, content };
    }

    private generateController(workflow: TopLevelWorkflow, options: TopLevelWorkflowGenerationOptions): { fileName: string; content: string } {
        const packageName = getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'workflows');
        const className = `${workflow.name}WorkflowController`;
        const workflowClass = `${workflow.name}Workflow`;
        const requestDtoClass = `${workflow.name}WorkflowRequestDto`;
        const fieldName = workflow.name.charAt(0).toLowerCase() + workflow.name.slice(1) + 'Workflow';
        const inputs = workflow.inputs || [];

        const executeArgs = inputs.map(i => {
            const cap = i.name.charAt(0).toUpperCase() + i.name.slice(1);
            return `request.get${cap}()`;
        }).join(', ');

        const content = `package ${packageName};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ${className} {

    @Autowired private ${workflowClass} ${fieldName};

    @PostMapping("/workflows/${workflow.name}")
    public void run(@RequestBody ${requestDtoClass} request) {
        ${fieldName}.execute(${executeArgs});
    }
}
`;
        return { fileName: `${className}.java`, content };
    }
}
