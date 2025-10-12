import { Aggregate } from "../../../../language/generated/ast.js";
import { ServiceGenerationOptions } from "./service-types.js";
import { ServiceStructureGenerator } from "./service-structure-generator.js";
import { ServiceCrudGenerator } from "./service-crud-generator.js";
import { ServiceBusinessGenerator } from "./service-business-generator.js";
import { ServiceQueryGenerator } from "./service-query-generator.js";
import { ServiceEventGenerator } from "./service-event-generator.js";

export function generateServiceCode(aggregate: Aggregate, projectName: string): string {
    const context = ServiceStructureGenerator.createServiceContext(aggregate, projectName);

    const imports = ServiceStructureGenerator.generateServiceImports(aggregate, projectName);
    const classDeclaration = ServiceStructureGenerator.generateClassDeclaration(context.aggregateName);
    const dependencies = ServiceStructureGenerator.generateDependencies(context.aggregateName, aggregate);
    const constructor = ServiceStructureGenerator.generateConstructor(context.aggregateName);

    const crudMethods = ServiceCrudGenerator.generateCrudMethods(context.capitalizedAggregate, context.rootEntity, projectName);
    const businessMethods = ServiceBusinessGenerator.generateBusinessMethods(
        context.capitalizedAggregate,
        aggregate,
        context.rootEntity,
        projectName
    );
    const customMethods = ServiceBusinessGenerator.generateCustomMethods(
        context.capitalizedAggregate,
        aggregate,
        projectName
    );
    const queryMethods = ServiceQueryGenerator.generateQueryMethods(context.capitalizedAggregate, aggregate);
    const eventProcessingMethods = ServiceEventGenerator.generateEventProcessingMethods(
        context.capitalizedAggregate,
        aggregate
    );

    return `package ${context.packageName};

${imports}

${classDeclaration}
${dependencies}

${constructor}

${crudMethods}

${businessMethods}

${customMethods}

${queryMethods}

${eventProcessingMethods}
}`;
}

export class ServiceGenerator {
    async generateService(aggregate: Aggregate, options: ServiceGenerationOptions): Promise<string> {
        if (!options || !options.projectName) {
            console.error('ServiceGenerator received options:', JSON.stringify(options));
            throw new Error(`projectName is required but was ${options?.projectName} for aggregate ${aggregate.name}`);
        }
        return generateServiceCode(aggregate, options.projectName);
    }
}
