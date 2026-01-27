import { Aggregate } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { getGlobalConfig } from "../../../common/config.js";
import { ServiceContext } from "./types.js";

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

        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.UserDto;`);
        
        // Import the root entity's DTO (e.g., AnswerDto, CourseDto)
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (rootEntity) {
            imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${rootEntity.name}Dto;`);
        }
        
        // Import projection DTOs for non-root entities (e.g., AnswerExecutionDto, AnswerUserDto)
        // These are needed for converting cross-aggregate DTOs to projection DTOs in create method
        aggregate.entities.forEach(entity => {
            if (!entity.isRoot) {
                imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${entity.name}Dto;`);
            }
        });
        
        // Import enum types used in root entity properties (needed for update methods)
        if (rootEntity && rootEntity.properties) {
            for (const prop of rootEntity.properties) {
                const propType = (prop as any).type;
                if (propType && propType.$type === 'EntityType' && propType.type) {
                    const typeName = propType.type.$refText || propType.type.ref?.name;
                    if (typeName && typeName.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/) && typeName !== 'AggregateState') {
                        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'enums')}.${typeName};`);
                    }
                }
            }
        }
        
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
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;');

        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', 'exception')}.${capitalize(projectName)}Exception;`);
        
        // Add CreateRequestDto import for create operations
        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'coordination', 'webapi', 'requestDtos')}.Create${capitalize(aggregateName)}RequestDto;`);
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
        const lowerAggregate = aggregateName.toLowerCase();
        const dependencies = [
            `    private static final Logger logger = LoggerFactory.getLogger(${capitalizedAggregate}Service.class);`,
            '',
            `    @Autowired`,
            `    private AggregateIdGeneratorService aggregateIdGeneratorService;`,
            '',
            `    @Autowired`,
            `    private UnitOfWorkService<UnitOfWork> unitOfWorkService;`,
            '',
            `    @Autowired`,
            `    private ${capitalizedAggregate}Repository ${lowerAggregate}Repository;`,
            '',
            `    @Autowired`,
            `    private ${capitalizedAggregate}Factory ${lowerAggregate}Factory;`
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
