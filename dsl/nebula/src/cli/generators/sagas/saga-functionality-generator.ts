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
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate.${rootEntity.name}Dto;`);

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
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate.${rootEntity.name}Dto;`);

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
            const steps = (func as any).functionalitySteps || [];
            for (const s of steps) {
                const stepName = s.stepName || 'step';
                const actions = s.stepActions || [];
                const call = actions.find((a: any) => a.serviceRef && a.method);
                let callExpr = '';
                if (call) {
                    const args = (call.args || []).map((x: any) => String(x)).join(', ');
                    callExpr = `this.${call.serviceRef}.${call.method}(${args})`;
                }
                let inner = '';
                if (resultType && callExpr) {
                    inner = `            this.result = ${callExpr};`;
                } else if (callExpr) {
                    inner = `            ${callExpr};`;
                }
                stepsBody += `        SagaSyncStep ${stepName}Step = new SagaSyncStep("${stepName}", () -> {\n${inner}\n        });\n        workflow.addStep(${stepName}Step);\n`;
            }

            const context = {
                packageName,
                imports,
                className,
                constructorDependencies,
                buildParams,
                resultType,
                stepsBody
            };

            const template = this.loadTemplate('saga/functionality.hbs');
            const content = this.renderTemplate(template, context);
            outputs[className + '.java'] = content;
        }

        return outputs;
    }

}