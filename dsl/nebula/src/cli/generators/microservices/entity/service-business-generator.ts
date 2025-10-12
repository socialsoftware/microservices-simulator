import { Aggregate, Entity, Method } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../shared/resolvers/type-resolver.js";

export class ServiceBusinessGenerator {
    static generateBusinessMethods(aggregateName: string, aggregate: Aggregate, rootEntity: Entity, projectName: string): string {
        if (!aggregate.methods || aggregate.methods.length === 0) {
            return '    // No business methods defined';
        }

        const entityMethods = aggregate.methods.filter(method => {
            const isServiceQuery = (method.name.startsWith('get') && method.name.includes('By')) ||
                (method.name.startsWith('find') && method.name.includes('By'));

            return !isServiceQuery;
        });

        const methods = entityMethods.map(method =>
            this.generateBusinessMethod(method, aggregateName, rootEntity, projectName)
        ).join('\n\n');

        return `    // Business Methods
${methods}`;
    }

    static generateCustomMethods(aggregateName: string, aggregate: Aggregate, projectName: string): string {
        if (!aggregate.workflows || aggregate.workflows.length === 0) {
            return '    // No custom workflows defined';
        }

        const methods = aggregate.workflows.map(workflow =>
            this.generateWorkflowMethod(workflow, aggregateName, projectName)
        ).join('\n\n');

        return `    // Custom Workflow Methods
${methods}`;
    }

    private static generateBusinessMethod(method: Method, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const methodName = method.name;

        const parameters = this.generateMethodParameters(method);
        const returnType = this.resolveReturnType(method);
        const methodBody = this.generateMethodBody(method, aggregateName, projectName, false);

        return `    @Transactional
    public ${returnType} ${methodName}(${parameters}) {
        try {
${methodBody}
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error in ${methodName}: " + e.getMessage());
        }
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
            `            // TODO: Implement workflow logic for ${methodName}
            throw new UnsupportedOperationException("Workflow ${methodName} not implemented");`;

        return `    @Transactional
    public ${returnType} ${methodName}(${parameters}) {
        try {
${workflowSteps}
${returnType !== 'void' ? '            return result;' : ''}
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error in workflow ${methodName}: " + e.getMessage());
        }
    }`;
    }

    private static generateWorkflowStep(step: any, aggregateName: string): string {
        const stepType = step.type || 'action';
        const stepName = step.name;

        if (!stepType) {
            console.warn(`Step type is undefined for aggregate ${aggregateName}`);
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
                return `// Execute ${stepName}
            execute${capitalize(stepName)}();`;
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
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id));
            
            // Business logic for ${method.name}`;

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
