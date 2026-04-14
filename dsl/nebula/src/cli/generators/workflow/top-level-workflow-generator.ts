import { capitalize } from "../../utils/generator-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "../common/unified-type-resolver.js";
import { getGlobalConfig } from "../common/config.js";

export class TopLevelWorkflowGenerator {

    static generate(workflow: any, projectName: string): { fileName: string; code: string; ownerAggregate: string } {
        const workflowName = workflow.name;
        const className = `${capitalize(workflowName)}FunctionalitySagas`;
        const lowerProject = projectName.toLowerCase();

        const inputs = (workflow.inputs || []) as any[];
        const steps = (workflow.workflowSteps || []) as any[];

        const lastStep = steps[steps.length - 1];
        const ownerAggregate = lastStep?.action?.target || steps[0]?.action?.target || 'Unknown';
        const lowerOwner = ownerAggregate.toLowerCase();

        const packageName = getGlobalConfig().buildPackageName(
            projectName, 'microservices', lowerOwner, 'coordination', 'sagas'
        );

        const inputParamList = inputs
            .map(inp => `${TypeResolver.resolveJavaType(inp.type)} ${inp.name}`)
            .join(', ');

        const targetServices = new Set<string>();
        for (const step of steps) {
            if (step.action?.target) targetServices.add(step.action.target);
            if (step.compensate?.target) targetServices.add(step.compensate.target);
        }

        const stepNameSet = new Set(steps.map((s: any) => s.name));
        const referencedStepResults = new Set<string>();
        const collectRefs = (args: any[]) => {
            for (const a of args) {
                if (stepNameSet.has(a.name)) referencedStepResults.add(a.name);
            }
        };
        for (const step of steps) {
            collectRefs((step.action?.args || []) as any[]);
            if (step.compensate) collectRefs((step.compensate.args || []) as any[]);
        }

        const stepReturnTypes = new Map<string, string>();
        for (const step of steps) {
            const target = step.action?.target;
            if (target && referencedStepResults.has(step.name)) {
                stepReturnTypes.set(step.name, `${capitalize(target)}Dto`);
            }
        }

        const stepDependencies = new Map<string, string[]>();
        for (const step of steps) {
            const deps: string[] = [];
            const checkArgs = (args: any[]) => {
                for (const a of args) {
                    if (stepNameSet.has(a.name) && a.name !== step.name) {
                        deps.push(a.name);
                    }
                }
            };
            checkArgs((step.action?.args || []) as any[]);
            stepDependencies.set(step.name, [...new Set(deps)]);
        }

        const basePackage = getGlobalConfig().getBasePackage();
        const imports = new Set<string>();
        imports.add(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.add(`import ${basePackage}.ms.coordination.workflow.command.CommandGateway;`);
        imports.add(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.add(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.add(`import ${basePackage}.ms.sagas.workflow.SagaStep;`);
        imports.add(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);

        imports.add(`import ${basePackage}.${lowerProject}.ServiceMapping;`);

        for (const step of steps) {
            const t = step.action?.target;
            const m = step.action?.method;
            if (t && m) {
                const capM = m.charAt(0).toUpperCase() + m.slice(1);
                const capT = t.charAt(0).toUpperCase() + t.slice(1);
                const crudNames: Record<string, string> = {
                    [`create${capT}`]: `Create${capT}Command`,
                    [`get${capT}ById`]: `Get${capT}ByIdCommand`,
                    [`getAll${capT}s`]: `GetAll${capT}sCommand`,
                    [`update${capT}`]: `Update${capT}Command`,
                    [`delete${capT}`]: `Delete${capT}Command`,
                };
                const cmdClass = crudNames[m] || `${capM}${capT}Command`;
                imports.add(`import ${basePackage}.${lowerProject}.command.${t.toLowerCase()}.${cmdClass};`);
            }
            if (step.compensate) {
                const ct = step.compensate.target;
                const cm = step.compensate.method;
                const capCM = cm.charAt(0).toUpperCase() + cm.slice(1);
                const capCT = ct.charAt(0).toUpperCase() + ct.slice(1);
                const crudNames2: Record<string, string> = {
                    [`create${capCT}`]: `Create${capCT}Command`,
                    [`get${capCT}ById`]: `Get${capCT}ByIdCommand`,
                    [`getAll${capCT}s`]: `GetAll${capCT}sCommand`,
                    [`update${capCT}`]: `Update${capCT}Command`,
                    [`delete${capCT}`]: `Delete${capCT}Command`,
                };
                const compCmdClass = crudNames2[cm] || `${capCM}${capCT}Command`;
                imports.add(`import ${basePackage}.${lowerProject}.command.${ct.toLowerCase()}.${compCmdClass};`);
            }
        }

        for (const [, javaType] of stepReturnTypes) {
            imports.add(`import ${basePackage}.${lowerProject}.shared.dtos.${javaType};`);
        }

        const hasDeps = [...stepDependencies.values()].some(d => d.length > 0);
        if (hasDeps) {
            imports.add('import java.util.ArrayList;');
            imports.add('import java.util.Arrays;');
        }

        const serviceFields: string[] = [];

        const resultFields: string[] = [];
        const resultGettersSetters: string[] = [];
        for (const [stepName, javaType] of stepReturnTypes) {
            const fieldName = `${stepName}Result`;
            resultFields.push(`    private ${javaType} ${fieldName};`);
            const capField = fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
            resultGettersSetters.push(`    public ${javaType} get${capField}() { return ${fieldName}; }`);
            resultGettersSetters.push(`    public void set${capField}(${javaType} ${fieldName}) { this.${fieldName} = ${fieldName}; }`);
        }

        const ctorParams: string[] = ['SagaUnitOfWorkService unitOfWorkService'];
        if (inputParamList.length > 0) ctorParams.push(inputParamList);
        ctorParams.push('SagaUnitOfWork unitOfWork');
        ctorParams.push('CommandGateway commandGateway');

        const ctorAssignments: string[] = [
            '        this.unitOfWorkService = unitOfWorkService;',
            '        this.commandGateway = commandGateway;'
        ];

        const inputPassList = inputs.map(i => i.name).join(', ');
        const buildWorkflowCallArgs = inputPassList.length > 0
            ? `${inputPassList}, unitOfWork`
            : 'unitOfWork';

        const buildWorkflowParams = inputs
            .map(inp => `${TypeResolver.resolveJavaType(inp.type)} ${inp.name}`)
            .join(', ');
        const buildWorkflowParamsFull = buildWorkflowParams.length > 0
            ? `${buildWorkflowParams}, SagaUnitOfWork unitOfWork`
            : 'SagaUnitOfWork unitOfWork';

        const renderArg = (arg: any): string => {
            const head = arg.name;
            const chain = (arg.chain || []) as string[];
            if (referencedStepResults.has(head)) {
                const accessors = chain.map(c => `.get${capitalize(c)}()`).join('');
                return `this.${head}Result${accessors}`;
            }
            if (chain.length > 0) {
                const accessors = chain.map(c => `.get${capitalize(c)}()`).join('');
                return `${head}${accessors}`;
            }
            return head;
        };

        const stepLines: string[] = [];
        const addStepLines: string[] = [];

        for (const step of steps) {
            const stepVar = `${step.name}Step`;
            const target = step.action?.target;
            const method = step.action?.method;
            const args = (step.action?.args || []) as any[];
            const argList = args.map(a => renderArg(a)).join(', ');
            const argSep = argList.length > 0 ? ', ' : '';

            const capMethod = method.charAt(0).toUpperCase() + method.slice(1);
            const capTarget = target.charAt(0).toUpperCase() + target.slice(1);

            const crudCommandNames: Record<string, string> = {
                [`create${capTarget}`]: `Create${capTarget}Command`,
                [`get${capTarget}ById`]: `Get${capTarget}ByIdCommand`,
                [`getAll${capTarget}s`]: `GetAll${capTarget}sCommand`,
                [`update${capTarget}`]: `Update${capTarget}Command`,
                [`delete${capTarget}`]: `Delete${capTarget}Command`,
            };
            const commandClassName = crudCommandNames[method] || `${capMethod}${capTarget}Command`;
            const serviceMapping = `ServiceMapping.${target.toUpperCase()}.getServiceName()`;

            let actionBody: string;
            const cmdVar = `${step.name}Cmd`;
            const cmdConstruction = `${commandClassName} ${cmdVar} = new ${commandClassName}(unitOfWork, ${serviceMapping}${argSep}${argList});`;
            if (referencedStepResults.has(step.name)) {
                const returnType = stepReturnTypes.get(step.name) || 'Object';
                actionBody = `${cmdConstruction}\n            this.${step.name}Result = (${returnType}) commandGateway.send(${cmdVar});`;
            } else {
                actionBody = `${cmdConstruction}\n            commandGateway.send(${cmdVar});`;
            }

            const deps = stepDependencies.get(step.name) || [];
            const depArg = deps.length > 0
                ? `, new ArrayList<>(Arrays.asList(${deps.map(d => `${d}Step`).join(', ')}))`
                : '';

            stepLines.push(
                `        SagaStep ${stepVar} = new SagaStep("${step.name}", () -> {` +
                `\n            ${actionBody}` +
                `\n        }${depArg});`
            );

            if (step.compensate) {
                const cTarget = step.compensate.target;
                const cMethod = step.compensate.method;
                const cArgs = (step.compensate.args || []) as any[];
                const cArgList = cArgs.map(a => renderArg(a)).join(', ');
                const cArgSep = cArgList.length > 0 ? ', ' : '';

                const cCapMethod = cMethod.charAt(0).toUpperCase() + cMethod.slice(1);
                const cCapTarget = cTarget.charAt(0).toUpperCase() + cTarget.slice(1);
                const cCrudCommandNames: Record<string, string> = {
                    [`create${cCapTarget}`]: `Create${cCapTarget}Command`,
                    [`get${cCapTarget}ById`]: `Get${cCapTarget}ByIdCommand`,
                    [`getAll${cCapTarget}s`]: `GetAll${cCapTarget}sCommand`,
                    [`update${cCapTarget}`]: `Update${cCapTarget}Command`,
                    [`delete${cCapTarget}`]: `Delete${cCapTarget}Command`,
                };
                const cCommandClassName = cCrudCommandNames[cMethod] || `${cCapMethod}${cCapTarget}Command`;
                const cServiceMapping = `ServiceMapping.${cTarget.toUpperCase()}.getServiceName()`;

                stepLines.push(
                    `        ${stepVar}.registerCompensation(() -> {` +
                    `\n            ${cCommandClassName} compCmd = new ${cCommandClassName}(unitOfWork, ${cServiceMapping}${cArgSep}${cArgList});` +
                    `\n            commandGateway.send(compCmd);` +
                    `\n        }, unitOfWork);`
                );
            }

            stepLines.push('');
            addStepLines.push(`        this.workflow.addStep(${stepVar});`);
        }

        const code = `package ${packageName};

${Array.from(imports).sort().join('\n')}

public class ${className} extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
${serviceFields.join('\n')}
${resultFields.length > 0 ? '\n' + resultFields.join('\n') : ''}

    public ${className}(${ctorParams.join(',\n            ')}) {
${ctorAssignments.join('\n')}
        this.buildWorkflow(${buildWorkflowCallArgs});
    }

    public void buildWorkflow(${buildWorkflowParamsFull}) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

${stepLines.join('\n')}
${addStepLines.join('\n')}
    }

${resultGettersSetters.join('\n')}
}
`;

        return { fileName: `${className}.java`, code, ownerAggregate };
    }
}
