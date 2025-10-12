import { Aggregate, Entity, Method } from "../../../language/generated/ast.js";
import { TypeResolver } from "../common/resolvers/type-resolver.js";
import * as fs from 'fs/promises';
import * as path from 'path';
import { fileURLToPath } from 'node:url';
import { getGlobalConfig } from "../common/config.js";
const __dirname = path.dirname(fileURLToPath(import.meta.url));

export interface SagaFunctionalityGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export class SagaFunctionalityGenerator {
    constructor() {
    }

    async generateSagaFunctionality(aggregate: Aggregate, options: SagaFunctionalityGenerationOptions): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string } = {};

        results['saga-functionality'] = await this.generateSagaFunctionalityClass(aggregate, rootEntity, options);

        return results;
    }

    private async generateSagaFunctionalityClass(aggregate: Aggregate, rootEntity: Entity, options: SagaFunctionalityGenerationOptions): Promise<string> {
        const context = this.buildSagaFunctionalityContext(aggregate, rootEntity, options);
        const template = await this.getSagaFunctionalityTemplate();
        return this.renderTemplate(template, context);
    }

    private buildSagaFunctionalityContext(aggregate: Aggregate, rootEntity: Entity, options: SagaFunctionalityGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const sagaMethods = this.buildSagaMethods(aggregate, rootEntity);

        const imports = this.buildSagaFunctionalityImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: getGlobalConfig().buildPackageName(options.projectName, 'sagas', 'coordination', lowerAggregate),
            sagaMethods,
            imports
        };
    }

    private buildSagaMethods(aggregate: Aggregate, rootEntity: Entity): any[] {
        const methods: any[] = [];

        if (rootEntity.methods) {
            for (const method of rootEntity.methods) {
                const methodName = method.name;
                const capitalizedMethod = this.capitalize(methodName);
                const returnType = this.getReturnType(method);
                const parameters = this.buildMethodParameters(method);

                methods.push({
                    methodName,
                    capitalizedMethod,
                    returnType,
                    parameters,
                    parameterList: this.buildParameterList(method)
                });
            }
        }

        return methods;
    }

    private buildMethodParameters(method: Method): any[] {
        const parameters: any[] = [];

        if (method.parameters) {
            for (const param of method.parameters) {
                let paramType = TypeResolver.resolveJavaType(param.type);
                if (paramType === 'UnitOfWork') {
                    paramType = 'SagaUnitOfWork';
                }
                const paramName = param.name;

                parameters.push({
                    type: paramType,
                    name: paramName
                });
            }
        }

        return parameters;
    }

    private buildParameterList(method: Method): string {
        if (!method.parameters || method.parameters.length === 0) {
            return '';
        }

        return method.parameters
            .map(param => {
                let paramType = TypeResolver.resolveJavaType(param.type);
                if (paramType === 'UnitOfWork') {
                    paramType = 'SagaUnitOfWork';
                }
                return `${paramType} ${param.name}`;
            })
            .join(', ');
    }

    private getReturnType(method: Method): string {
        if (method.returnType) {
            return TypeResolver.resolveJavaType(method.returnType);
        }
        return 'void';
    }

    private buildSagaFunctionalityImports(aggregate: Aggregate, options: SagaFunctionalityGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import java.time.LocalDateTime;');
        imports.push('import org.springframework.stereotype.Component;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;');

        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        imports.push(`import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', lowerAggregate, 'service')}.${capitalizedAggregate}Service;`);
        imports.push(`import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', lowerAggregate, 'aggregate')}.${rootEntityName}Dto;`);
        imports.push(`import ${getGlobalConfig().buildPackageName(options.projectName, 'sagas', 'aggregates', 'dtos')}.Saga${capitalizedAggregate}Dto;`);

        return imports;
    }

    private async getSagaFunctionalityTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-functionality.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

    private renderTemplate(template: string, context: any): string {
        let result = template;

        result = result.replace(/\{\{packageName\}\}/g, context.packageName || '');
        result = result.replace(/\{\{aggregateName\}\}/g, context.aggregateName || '');
        result = result.replace(/\{\{lowerAggregate\}\}/g, context.lowerAggregate || '');

        const importsText = (context.imports || []).map((imp: string) => imp).join('\n');
        result = result.replace(/\{\{#each imports\}\}[\s\S]*?\{\{\/each\}\}/g, importsText);

        const sagaMethodsText = (context.sagaMethods || []).map((method: any) => {
            return `    public ${method.returnType} ${method.methodName}(${method.parameterList}) {
        // TODO: Implement saga functionality for ${method.methodName}
        // This method should orchestrate the saga workflow
        return null;
    }`;
        }).join('\n\n');
        result = result.replace(/\{\{#each sagaMethods\}\}[\s\S]*?\{\{\/each\}\}/g, sagaMethodsText);

        return result;
    }

    private capitalize(str: string): string {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
}
