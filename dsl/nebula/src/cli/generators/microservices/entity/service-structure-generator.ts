import { Aggregate } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { getGlobalConfig } from "../../shared/config.js";
import { ServiceContext } from "./service-types.js";

export class ServiceStructureGenerator {
    static generateServiceImports(aggregate: Aggregate, projectName: string): string {
        if (!projectName) {
            throw new Error(`projectName is undefined in generateServiceImports for aggregate ${aggregate?.name}`);
        }
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();

        const imports = [
            'import org.springframework.beans.factory.annotation.Autowired;',
            'import org.springframework.stereotype.Service;',
            'import org.springframework.transaction.annotation.Transactional;',
            'import org.slf4j.Logger;',
            'import org.slf4j.LoggerFactory;',
            '',
            `import ${getGlobalConfig().buildPackageName(projectName, 'microservices', lowerAggregate, 'aggregate')}.*;`,
            ''
        ];

        aggregate.entities.forEach(entity => {
            if (!entity.isRoot) {
                imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', lowerAggregate, 'aggregate')}.${entity.name};`);
            }
        });

        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;');
        imports.push('');

        const hasCollections = aggregate.entities.some(entity =>
            entity.properties?.some(prop => prop.type && this.isCollectionType(prop.type))
        );

        if (hasCollections) {
            imports.push('import java.util.*;');
            imports.push('import java.util.stream.Collectors;');
            imports.push('');
        }

        imports.push('import java.util.List;');
        imports.push('import java.util.stream.Collectors;');

        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', 'user', 'aggregate')}.UserDto;`);
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;');

        const hasDateTime = aggregate.entities.some(entity =>
            entity.properties?.some(prop => {
                if (!prop.type) return false;
                if (typeof prop.type === 'string') {
                    return prop.type === 'LocalDateTime';
                }
                if (prop.type.$type === 'PrimitiveType') {
                    return prop.type.typeName === 'LocalDateTime';
                }
                return false;
            })
        );

        if (hasDateTime) {
            imports.push('import java.time.LocalDateTime;');
            imports.push('');
        }

        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;');

        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', 'exception')}.${capitalize(projectName)}Exception;`);
        imports.push('');

        return imports.join('\n');
    }

    static generateClassDeclaration(aggregateName: string): string {
        return `@Service
@Transactional
public class ${capitalize(aggregateName)}Service {`;
    }

    static generateDependencies(aggregateName: string, aggregate: Aggregate): string {
        const capitalizedAggregate = capitalize(aggregateName);
        const dependencies = [
            `    private static final Logger logger = LoggerFactory.getLogger(${capitalizedAggregate}Service.class);`,
            '',
            `    @Autowired
    private ${capitalizedAggregate}Repository ${aggregateName.toLowerCase()}Repository;`,
            '',
            `    @Autowired
    private ${capitalizedAggregate}Factory ${aggregateName.toLowerCase()}Factory;`
        ];


        return dependencies.join('\n');
    }

    static generateConstructor(aggregateName: string): string {
        const capitalizedAggregate = capitalize(aggregateName);
        return `    public ${capitalizedAggregate}Service() {}`;
    }

    static createServiceContext(aggregate: Aggregate, projectName: string): ServiceContext {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = capitalize(aggregateName);
        const packageName = `${getGlobalConfig().buildPackageName(projectName, 'microservices', aggregateName.toLowerCase(), 'service')}`;

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregateName}`);
        }

        return {
            aggregateName,
            capitalizedAggregate,
            packageName,
            rootEntity,
            projectName
        };
    }

    private static isCollectionType(type: any): boolean {
        if (!type) return false;
        if (typeof type === 'string') {
            return type.toLowerCase().includes('list') || type.toLowerCase().includes('set');
        }
        return type.$type === 'CollectionType' || type.type === 'Collection';
    }
}
