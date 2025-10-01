import { Aggregate, CustomRepository, RepositoryMethod } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { getGlobalConfig } from "../../base/config.js";

export function generateRepositoryCode(aggregate: Aggregate, projectName: string): string {
    const aggregateName = aggregate.name;
    const capitalizedAggregate = capitalize(aggregateName);
    const packageName = `${getGlobalConfig().buildPackageName(projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate')}`;

    const imports = generateRepositoryImports(aggregate.customRepository);
    const interfaceDeclaration = generateInterfaceDeclaration(capitalizedAggregate);
    const methods = generateCustomRepositoryMethods(aggregate, capitalizedAggregate);

    return `package ${packageName};

${imports}

${interfaceDeclaration}
${methods}
}`;
}

function generateRepositoryImports(customRepository: CustomRepository | undefined): string {
    const imports = new Set<string>();

    imports.add('import java.util.Optional;');
    imports.add('import java.util.List;');
    imports.add('import java.util.Set;');

    if (customRepository && customRepository.repositoryMethods) {
        customRepository.repositoryMethods.forEach(method => {
        });
    }

    return Array.from(imports).join('\n');
}

function generateInterfaceDeclaration(aggregateName: string): string {
    return `public interface ${aggregateName}CustomRepository {`;
}

function generateCustomRepositoryMethods(aggregate: Aggregate, capitalizedAggregate: string): string {
    if (aggregate.customRepository && aggregate.customRepository.repositoryMethods.length > 0) {
        return aggregate.customRepository.repositoryMethods
            .map(method => generateRepositoryMethod(method))
            .join('\n');
    }

    return '';
}

function generateRepositoryMethod(method: RepositoryMethod): string {
    const returnType = resolveRepositoryReturnType(method.returnType);
    const params = method.parameters
        .map(param => {
            const javaType = resolveParameterType(param.type);
            return `${javaType} ${param.name}`;
        })
        .join(', ');

    return `    ${returnType} ${method.name}(${params});`;
}

function resolveRepositoryReturnType(returnType: any): string {
    if (!returnType) return 'void';

    if (returnType.$cstNode && returnType.$cstNode.text) {
        const text = returnType.$cstNode.text.trim();
        if (text) return text;
    }

    if (returnType.type) {
        const innerType = resolveParameterType(returnType.type);

        const cstText = returnType.$cstNode?.text || '';

        if (cstText.startsWith('Optional<')) {
            return `Optional<${innerType}>`;
        } else if (cstText.startsWith('List<')) {
            return `List<${innerType}>`;
        } else if (cstText.startsWith('Set<')) {
            return `Set<${innerType}>`;
        }

        return innerType;
    }

    if (returnType.name) {
        return returnType.name;
    }

    return 'Object';
}

function resolveParameterType(type: any): string {
    if (!type) return 'Object';

    if (typeof type === 'string') {
        return type;
    }

    if (type.name) {
        return type.name;
    }

    if (type.$type) {
        if (type.$type === 'PrimitiveType' || type.$type === 'BuiltinType' || type.$type === 'EntityType') {
            if (type.name) return type.name;
        }
    }

    if (type.$cstNode && type.$cstNode.text) {
        return type.$cstNode.text;
    }

    return 'Object';
}


export class RepositoryGenerator {
    async generateRepository(aggregate: Aggregate, options: { projectName: string }): Promise<string> {
        return generateRepositoryCode(aggregate, options.projectName);
    }
}