import { OrchestrationBase } from '../common/orchestration-base.js';

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

        return outputs;
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