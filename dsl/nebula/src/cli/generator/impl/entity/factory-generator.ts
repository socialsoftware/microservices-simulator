import { Aggregate } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { getGlobalConfig } from "../../base/config.js";

export function generateFactoryCode(aggregate: Aggregate, projectName: string): string {
    const aggregateName = aggregate.name;
    const capitalizedAggregate = capitalize(aggregateName);
    const packageName = `${getGlobalConfig().buildPackageName(projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate')}`;

    const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
    if (!rootEntity) {
        throw new Error(`No root entity found in aggregate ${aggregateName}`);
    }

    const imports = generateFactoryImports(projectName);
    const classDeclaration = generateFactoryClassDeclaration(capitalizedAggregate);
    const createMethods = generateCreateMethods(capitalizedAggregate, rootEntity);
    const dtoMethods = generateDtoMethods(capitalizedAggregate, rootEntity);

    return `package ${packageName};

${imports}

${classDeclaration}

${createMethods}

${dtoMethods}
}`;
}

function generateFactoryImports(projectName: string): string {
    return `import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;`;
}

function generateFactoryClassDeclaration(aggregateName: string): string {
    return `@Service
public class ${aggregateName}Factory {`;
}

function generateCreateMethods(aggregateName: string, rootEntity: any): string {
    const lowerName = aggregateName.toLowerCase();
    const dtoName = `${rootEntity.name}Dto`;

    const properties = rootEntity.properties || [];
    const constructorParams = properties
        .map((prop: any) => `${lowerName}Dto.get${capitalize(prop.name)}()`)
        .join(',\n            ');

    return `    public ${aggregateName} create${aggregateName}(Integer aggregateId, ${dtoName} ${lowerName}Dto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new ${rootEntity.name}(${constructorParams ? '\n            ' + constructorParams + '\n        ' : ''});
    }

    public ${aggregateName} create${aggregateName}FromExisting(${aggregateName} existing${aggregateName}) {
        // Create a copy of the existing aggregate
        if (existing${aggregateName} instanceof ${rootEntity.name}) {
            return new ${rootEntity.name}((${rootEntity.name}) existing${aggregateName});
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }`;
}

function generateDtoMethods(aggregateName: string, rootEntity: any): string {
    const dtoName = `${rootEntity.name}Dto`;

    return `    public ${dtoName} create${dtoName}(${aggregateName} ${aggregateName.toLowerCase()}) {
        return new ${dtoName}((${rootEntity.name}) ${aggregateName.toLowerCase()});
    }`;
}

export class FactoryGenerator {
    async generateFactory(aggregate: Aggregate, options: { projectName: string }): Promise<string> {
        return generateFactoryCode(aggregate, options.projectName);
    }
}