import { Aggregate } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { getGlobalConfig } from "../../../common/config.js";
import { ServiceContext } from "./types.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";

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
            '',
            `import ${getGlobalConfig().buildPackageName(projectName, 'microservices', lowerAggregate, 'aggregate')}.*;`,
            ''
        ];

        
        imports.push('import java.util.List;');
        imports.push('import java.util.Set;');
        imports.push('import java.util.stream.Collectors;');
        
        
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (rootEntity) {
            imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${rootEntity.name}Dto;`);
        }
        
        
        
        aggregate.entities.forEach(entity => {
            if (!entity.isRoot) {
                imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${entity.name}Dto;`);
            }
        });
        
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

        
        if (rootEntity && rootEntity.properties) {
            for (const prop of rootEntity.properties) {
                const propName = (prop as any).name?.toLowerCase?.() ?? '';
                
                if (propName === 'id') {
                    continue;
                }
                
                if ((prop as any).isFinal) {
                    continue;
                }

                const javaType = TypeResolver.resolveJavaType((prop as any).type);
                const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
                
                const isEntityType = TypeResolver.isEntityType(javaType);
                if (isCollection || isEntityType) {
                    continue;
                }

                
                const propType = (prop as any).type;
                if (propType && propType.$type === 'EntityType' && propType.type) {
                    const typeName = propType.type.$refText || propType.type.ref?.name;
                    if (
                        typeName &&
                        typeName !== 'AggregateState' &&
                        /^[A-Z][a-zA-Z]*(Type|State|Role)$/.test(typeName)
                    ) {
                        imports.push(
                            `import ${getGlobalConfig().buildPackageName(
                                projectName,
                                'shared',
                                'enums'
                            )}.${typeName};`
                        );
                    }
                }
            }
        }

        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;');

        
        const eventsPackage = getGlobalConfig().buildPackageName(projectName, 'microservices', lowerAggregate, 'events', 'publish');
        imports.push(`import ${eventsPackage}.${aggregateName}DeletedEvent;`);
        imports.push(`import ${eventsPackage}.${aggregateName}UpdatedEvent;`);

        
        const projectionEntities = (aggregate.entities || []).filter((e: any) =>
            !e.isRoot && e.aggregateRef
        );

        projectionEntities.forEach((projEntity: any) => {
            const projEntityName = projEntity.name;
            imports.push(`import ${eventsPackage}.${projEntityName}DeletedEvent;`);
            imports.push(`import ${eventsPackage}.${projEntityName}UpdatedEvent;`);
        });

        
        const rootEntityForCollections = aggregate.entities?.find((e: any) => e.isRoot);
        if (rootEntityForCollections && rootEntityForCollections.properties) {
            for (const prop of rootEntityForCollections.properties) {
                const propType = (prop as any).type;
                
                const javaType = TypeResolver.resolveJavaType(propType);
                if (javaType && (javaType.startsWith('Set<') || javaType.startsWith('List<'))) {
                    const elementType = TypeResolver.getElementType(propType);
                    if (elementType && TypeResolver.isEntityType(javaType)) {
                        
                        imports.push(`import ${eventsPackage}.${elementType}RemovedEvent;`);
                        imports.push(`import ${eventsPackage}.${elementType}UpdatedEvent;`);
                    }
                }
            }
        }

        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', 'exception')}.${capitalize(projectName)}Exception;`);
        
        
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

}
