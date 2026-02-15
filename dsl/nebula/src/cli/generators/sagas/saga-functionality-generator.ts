import { initializeAggregateProperties, getWorkflows } from '../../utils/aggregate-helpers.js';
import { SagaCrudGenerator } from './saga-crud-generator.js';
import { SagaCollectionGenerator } from './saga-collection-generator.js';
import { SagaWorkflowGenerator } from './saga-workflow-generator.js';
import { SagaEventProcessingGenerator } from './saga-event-processing-generator.js';
import { SagaHelpers } from './saga-helpers.js';
import type { Aggregate } from '../../../language/generated/ast.js';
import { SagaGenerationOptions } from './saga-generator.js';
import { StringUtils } from '../../utils/string-utils.js';
import { TemplateManager } from '../../utils/template-manager.js';



export class SagaFunctionalityGenerator {
    private templateManager = TemplateManager.getInstance();
    private crudGenerator = new SagaCrudGenerator();
    private collectionGenerator = new SagaCollectionGenerator();
    private workflowGenerator = new SagaWorkflowGenerator();
    private eventProcessingGenerator = new SagaEventProcessingGenerator();
    private helpers = new SagaHelpers();

    private getBasePackage(options: SagaGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in SagaGenerationOptions');
        }
        return options.basePackage;
    }

    

    generateForAggregate(aggregate: any, options: SagaGenerationOptions, allAggregates?: Aggregate[]): Record<string, string> {
        const outputs: Record<string, string> = {};
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const packageName = `${basePackage}.${options.projectName.toLowerCase()}.sagas.coordination.${lowerAggregate}`;

        
        initializeAggregateProperties(aggregate);
        const allWorkflows = getWorkflows(aggregate);

        
        if (aggregate.generateCrud) {
            
            const aggregatesToUse = allAggregates || (aggregate.$container as any)?.aggregates || [];
            const crudSagas = this.crudGenerator.generateCrudSagaFunctionalities(aggregate, options, packageName, aggregatesToUse);
            Object.assign(outputs, crudSagas);

            
            const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot);
            if (rootEntity) {
                const collectionSagas = this.collectionGenerator.generateCollectionSagaFunctionalities(
                    aggregate,
                    rootEntity,
                    options,
                    packageName
                );
                Object.assign(outputs, collectionSagas);
            }
        }

        
        this.generateEndpointSagaFunctionalities(aggregate, options, packageName, allWorkflows, outputs);

        
        this.generateFunctionalityMethodSagas(aggregate, options, packageName, outputs);

        
        this.eventProcessingGenerator.generateEventProcessingSagaFunctionalities(aggregate, options, outputs);

        return outputs;
    }

    

    private generateEndpointSagaFunctionalities(
        aggregate: any,
        options: SagaGenerationOptions,
        packageName: string,
        allWorkflows: any[],
        outputs: Record<string, string>
    ): void {
        const endpoints = aggregate.webApiEndpoints?.endpoints || [];

        for (const endpoint of endpoints) {
            const methodName: string = endpoint.methodName;
            if (!methodName) continue;

            const className = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;

            
            const matchingWorkflow = allWorkflows.find((w: any) =>
                w.name === methodName && w.workflowSteps && w.workflowSteps.length > 0
            );

            if (matchingWorkflow) {
                
                const content = this.workflowGenerator.generateWorkflowFunctionality(aggregate, matchingWorkflow, options, packageName);
                outputs[className + '.java'] = content;
                continue;
            }

            
            const content = this.generateBasicEndpointSaga(aggregate, endpoint, options, packageName);
            outputs[className + '.java'] = content;
        }
    }

    

    private generateBasicEndpointSaga(
        aggregate: any,
        endpoint: any,
        options: SagaGenerationOptions,
        packageName: string
    ): string {
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const methodName = endpoint.methodName;
        const className = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;
        const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name };

        const imports: string[] = [];
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${StringUtils.capitalize(aggregate.name)}Service;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);

        const constructorDependencies = [
            { type: `${StringUtils.capitalize(aggregate.name)}Service`, name: `${lowerAggregate}Service` },
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

        return this.templateManager.renderTemplate('saga/functionality.hbs', context);
    }

    

    private generateFunctionalityMethodSagas(
        aggregate: any,
        options: SagaGenerationOptions,
        packageName: string,
        outputs: Record<string, string>
    ): void {
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const functionalityBlock = (aggregate as any).functionalities;
        const functionalityMethods = functionalityBlock?.functionalityMethods || [];

        for (const func of functionalityMethods) {
            const methodName: string = func.name;
            if (!methodName) continue;

            const className = `${StringUtils.capitalize(methodName)}FunctionalitySagas`;
            const rootEntity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name };

            const imports: string[] = [];
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${StringUtils.capitalize(aggregate.name)}Service;`);
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);

            const constructorDependencies: Array<{ type: string; name: string }> = [
                { type: `${StringUtils.capitalize(aggregate.name)}Service`, name: `${lowerAggregate}Service` },
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

            
            const { stepsBody, variableDecls } = this.generateFunctionalityStepsBody(func, resultType, imports);

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

            outputs[className + '.java'] = this.templateManager.renderTemplate('saga/functionality.hbs', context);
        }
    }

    

    private generateFunctionalityStepsBody(func: any, resultType: string | undefined, imports: string[]): { stepsBody: string; variableDecls: string } {
        const stepMap = new Map<string, string>();
        const steps = (func as any).functionalitySteps || [];
        const variableDeclarations: string[] = [];

        if (steps.length === 0) {
            return { stepsBody: '', variableDecls: '' };
        }

        let stepsBody = '';

        
        for (const s of steps) {
            const stepName = s.stepName || 'step';
            stepMap.set(stepName, `${stepName}Step`);
        }

        
        for (const s of steps) {
            const stepName = s.stepName || 'step';
            const stepVar = stepMap.get(stepName)!;
            const actions = s.stepActions || [];
            const dependencies = s.dependsOn?.dependencies || [];
            const compensation = s.compensation;

            
            let stepBody = '';

            for (const action of actions) {
                if (action.$type === 'FuncCallServiceAction') {
                    const serviceRef = action.serviceRef;
                    const method = action.method;
                    const args = (action.args || []).map((arg: any) => this.helpers.convertStepArgument(arg)).join(', ');
                    const callExpr = `this.${serviceRef}.${method}(${args})`;

                    if (action.assignTo) {
                        const varName = action.assignTo;
                        const varType = this.helpers.inferVariableType(method, resultType);
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
                    const expr = this.helpers.convertStepExpression(action.expression);
                    const varType = this.helpers.inferVariableTypeFromExpression(expr);
                    if (!variableDeclarations.includes(varName)) {
                        variableDeclarations.push(`private ${varType} ${varName};`);
                    }
                    stepBody += `            ${varName} = ${expr};\n`;
                } else if (action.$type === 'FuncRegisterSagaStateAction') {
                    const aggId = this.helpers.convertStepArgument(action.aggregateId);
                    const state = action.sagaState;
                    stepBody += `            unitOfWorkService.registerSagaState(${aggId}, ${state}, unitOfWork);\n`;
                }
            }

            
            let stepDependencies = '';
            if (dependencies.length > 0) {
                const depVars = dependencies.map((dep: string) => stepMap.get(dep) || `${dep}Step`).join(', ');
                stepDependencies = `, new ArrayList<>(Arrays.asList(${depVars}))`;
            }

            let stepCode = `        SagaSyncStep ${stepVar} = new SagaSyncStep("${stepName}", () -> {\n${stepBody}        }${stepDependencies});\n`;

            
            if (compensation && compensation.compensationActions) {
                let compensationBody = '';
                for (const compAction of compensation.compensationActions) {
                    if (compAction.$type === 'FuncCallServiceAction') {
                        const serviceRef = compAction.serviceRef;
                        const method = compAction.method;
                        const args = (compAction.args || []).map((arg: any) => this.helpers.convertStepArgument(arg)).join(', ');
                        compensationBody += `            this.${serviceRef}.${method}(${args});\n`;
                    } else if (compAction.$type === 'FuncRegisterSagaStateAction') {
                        const aggId = this.helpers.convertStepArgument(compAction.aggregateId);
                        const state = compAction.sagaState;
                        compensationBody += `            unitOfWorkService.registerSagaState(${aggId}, ${state}, unitOfWork);\n`;
                    }
                }
                stepCode += `        ${stepVar}.registerCompensation(() -> {\n${compensationBody}        }, unitOfWork);\n`;
            }

            stepCode += `        workflow.addStep(${stepVar});\n`;
            stepsBody += stepCode + '\n';
        }

        
        if (steps.some((s: any) => s.dependsOn?.dependencies?.length > 0)) {
            imports.push('import java.util.ArrayList;');
            imports.push('import java.util.Arrays;');
        }

        const variableDecls = Array.from(new Set(variableDeclarations)).join('\n    ');
        return { stepsBody, variableDecls };
    }
}
