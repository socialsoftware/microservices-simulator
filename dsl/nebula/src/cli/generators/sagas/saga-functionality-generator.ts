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

            const constructorDependencies = [
                { type: `${this.capitalize(aggregate.name)}Service`, name: `${lowerAggregate}Service` },
                { type: 'SagaUnitOfWorkService', name: 'sagaUnitOfWorkService' },
                { type: 'SagaUnitOfWork', name: 'unitOfWork' }
            ];

            let resultType: string | undefined;
            if (endpoint.returnType) {
                const rt = endpoint.returnType;
                if (typeof rt === 'string') {
                    resultType = rt.includes('Dto') ? rt : (rt === 'void' ? undefined : rt);
                } else if (rt.name) {
                    resultType = rt.name;
                }
            }

            const context = {
                packageName,
                imports,
                className,
                constructorDependencies,
                buildParams: [],
                resultType
            };

            const template = this.loadTemplate('saga/functionality.hbs');
            const content = this.renderTemplate(template, context);
            outputs[className + '.java'] = content;
        }

        return outputs;
    }

}