import { Aggregate, Entity, Method } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/string-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";
import { ActionMethodGenerator } from "./action-method-generator.js";

export class ServiceBusinessGenerator {
    static generateBusinessMethods(aggregateName: string, aggregate: Aggregate, rootEntity: Entity, projectName: string): string {
        if (!aggregate.methods || aggregate.methods.length === 0) {
            return '';
        }

        const methods = aggregate.methods.map(method => {
            if ((method as any).queryBody) {
                return this.generateQueryMethod(method, aggregateName, projectName, aggregate);
            }
            if (ActionMethodGenerator.hasActionBody(method)) {
                return ActionMethodGenerator.generate(method, aggregateName, rootEntity, projectName);
            }
            const isServiceQuery = (method.name.startsWith('get') && method.name.includes('By')) ||
                (method.name.startsWith('find') && method.name.includes('By'));
            if (isServiceQuery) {
                return '';
            }
            return this.generateBusinessMethod(method, aggregateName, rootEntity, projectName);
        }).filter(m => m.length > 0).join('\n\n');

        return methods;
    }

    static generateCustomMethods(aggregateName: string, aggregate: Aggregate, projectName: string): string {
        if (!aggregate.workflows || aggregate.workflows.length === 0) {
            return '';
        }

        const methods = aggregate.workflows.map(workflow =>
            this.generateWorkflowMethod(workflow, aggregateName, projectName)
        ).join('\n\n');

        return methods;
    }

    private static generateQueryMethod(method: Method, aggregateName: string, projectName: string, aggregate?: Aggregate): string {
        const queryBody = (method as any).queryBody;
        const repoMethodName = queryBody.repositoryMethod;
        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedAggregate = capitalize(aggregateName);

        const args = (queryBody.args || []).map((a: any) => {
            if (a.name) return a.name;
            if (a.stringValue !== undefined) return `"${a.stringValue}"`;
            if (a.literalValue !== undefined) return a.literalValue;
            return 'null';
        });

        const repoCall = `${lowerAggregate}Repository.${repoMethodName}(${args.join(', ')})`;

        const methodParams = (method.parameters || []).map((p: any) => {
            const pType = TypeResolver.resolveJavaType(p.type);
            return `${pType} ${p.name}`;
        });
        methodParams.push('UnitOfWork unitOfWork');

        const body = `            Set<Integer> aggregateIds = ${repoCall};
            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (${capitalizedAggregate}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(${lowerAggregate}Factory::create${capitalizedAggregate}Dto)
                .collect(java.util.stream.Collectors.toList());`;

        return `    @Transactional(isolation = Isolation.SERIALIZABLE)
    public java.util.List<${capitalizedAggregate}Dto> ${method.name}(${methodParams.join(', ')}) {
        try {
${body}
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error in ${method.name} ${capitalizedAggregate}: " + e.getMessage());
        }
    }`;
    }

    private static generateBusinessMethod(method: Method, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const methodName = method.name;

        const parameters = this.generateMethodParameters(method);
        const returnType = this.resolveReturnType(method);
        const methodBody = this.generateMethodBody(method, aggregateName, projectName, false);

        return `    @Transactional
    public ${returnType} ${methodName}(${parameters}) {
${ExceptionGenerator.generateTryCatchWrapper(projectName, `in ${methodName}`, aggregateName, methodBody)}
    }`;
    }

    private static generateWorkflowMethod(workflow: any, aggregateName: string, projectName: string): string {
        const methodName = workflow.name;

        const parameters = workflow.parameters ?
            workflow.parameters.map((param: any) =>
                `${TypeResolver.resolveJavaType(param.type)} ${param.name}`
            ).join(', ') : '';

        const returnType = workflow.returnType ?
            TypeResolver.resolveJavaType(workflow.returnType) : 'void';

        const workflowSteps = workflow.steps ?
            workflow.steps.map((step: any, index: number) =>
                `            // Step ${index + 1}: ${step.name}
            ${this.generateWorkflowStep(step, aggregateName)}`
            ).join('\n') :
            `            throw new UnsupportedOperationException("Workflow ${methodName} not implemented");`;

        const methodBody = workflowSteps + (returnType !== 'void' ? '\n            return result;' : '');

        return `    @Transactional
    public ${returnType} ${methodName}(${parameters}) {
${ExceptionGenerator.generateTryCatchWrapper(projectName, `in workflow ${methodName}`, aggregateName, methodBody)}
    }`;
    }

    private static generateWorkflowStep(step: any, aggregateName: string): string {
        const stepType = step.type || 'action';
        const stepName = step.name;

        if (!stepType) {
            return '// Step type was undefined';
        }
        switch (stepType.toLowerCase()) {
            case 'validation':
                return `if (!validate${capitalize(stepName)}()) {
                throw new ValidationException("Validation failed for ${stepName}");
            }`;
            case 'calculation':
                return `${stepName}Result = calculate${capitalize(stepName)}();`;
            case 'persistence':
                return `${aggregateName.toLowerCase()}Repository.save(${aggregateName.toLowerCase()});`;
            case 'notification':
                return `notificationService.send${capitalize(stepName)}Notification();`;
            default:
                return `execute${capitalize(stepName)}();`;
        }
    }

    private static generateMethodParameters(method: Method): string {
        if (!method.parameters || method.parameters.length === 0) {
            return 'Integer id';
        }

        const params = method.parameters.map(param => {
            const paramType = TypeResolver.resolveJavaType(param.type);
            const paramName = param.name || 'param';
            return `${paramType} ${paramName}`;
        });

        params.unshift('Integer id');

        return params.join(', ');
    }

    private static resolveReturnType(method: Method): string {
        if (!method.returnType) {
            return 'void';
        }

        return TypeResolver.resolveJavaType(method.returnType);
    }

    private static generateMethodBody(method: Method, aggregateName: string, projectName: string, isQuery: boolean): string {
        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityVar = lowerAggregate;

        let body = `            ${capitalizedAggregate} ${rootEntityVar} = ${lowerAggregate}Repository.findById(id)
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id));`;

        if (method.returnType) {
            const returnType = TypeResolver.resolveJavaType(method.returnType);
            body += `\n            ${returnType} result = ${rootEntityVar}.${method.name}();`;

            if (!isQuery) {
                body += `\n            ${lowerAggregate}Repository.save(${rootEntityVar});`;
            }

            body += '\n            return result;';
        } else {
            body += `\n            ${rootEntityVar}.${method.name}();`;

            if (!isQuery) {
                body += `\n            ${lowerAggregate}Repository.save(${rootEntityVar});`;
            }
        }

        return body;
    }
}
