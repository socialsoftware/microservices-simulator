import { Aggregate } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { getGlobalConfig } from "../../../common/config.js";
import { ServiceContext } from "./types.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { getEvents, findPreventReferencesTo, resolveUltimateSourceRoot } from "../../../../utils/aggregate-helpers.js";
import { EventNameParser } from "../../../common/utils/event-name-parser.js";

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



        const referencedEntityNames = new Set<string>();
        if (rootEntity?.properties) {
            for (const prop of rootEntity.properties) {
                const javaType = TypeResolver.resolveJavaType((prop as any).type);
                if (aggregate.entities.some(e => e.name === javaType)) {
                    referencedEntityNames.add(javaType);
                }
                const elementType = TypeResolver.getElementType((prop as any).type);
                if (elementType && aggregate.entities.some(e => e.name === elementType)) {
                    referencedEntityNames.add(elementType);
                }
            }
        }
        aggregate.entities.forEach(entity => {
            if (!entity.isRoot && referencedEntityNames.has(entity.name)) {
                imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${entity.name}Dto;`);
            }
        });

        imports.push('');


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

                const propType = (prop as any).type;
                if (propType && propType.$type === 'EntityType' && propType.type) {
                    const ref = propType.type.ref;
                    const typeName = propType.type.$refText || ref?.name;
                    const isEnum = (ref && (ref.$type === 'EnumDefinition' || ref.$type === 'Enum')) ||
                        (typeName && typeName !== 'AggregateState' && /^[A-Z][a-zA-Z]*(Type|State|Role|Status|Method|Kind|Mode|Level|Priority)$/.test(typeName));
                    if (typeName && typeName !== 'AggregateState' && isEnum) {
                        imports.push(
                            `import ${getGlobalConfig().buildPackageName(
                                projectName,
                                'shared',
                                'enums'
                            )}.${typeName};`
                        );
                        continue;
                    }
                }

                const isEntityType = TypeResolver.isEntityType(javaType);
                if (isCollection || isEntityType) {
                    continue;
                }
            }
        }

        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;');
        imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;');


        const eventsPackage = getGlobalConfig().buildPackageName(projectName, 'events');
        const eventImports = new Set<string>();

        eventImports.add(`import ${eventsPackage}.${aggregateName}DeletedEvent;`);
        eventImports.add(`import ${eventsPackage}.${aggregateName}UpdatedEvent;`);

        if (rootEntity?.properties) {
            for (const prop of rootEntity.properties) {
                const propType = (prop as any).type;

                const javaType = TypeResolver.resolveJavaType(propType);
                if (javaType && (javaType.startsWith('Set<') || javaType.startsWith('List<'))) {
                    const elementType = TypeResolver.getElementType(propType);
                    if (elementType && TypeResolver.isEntityType(javaType)) {
                        eventImports.add(`import ${eventsPackage}.${elementType}RemovedEvent;`);
                        eventImports.add(`import ${eventsPackage}.${elementType}UpdatedEvent;`);
                    }
                }
            }
        }

        const events = getEvents(aggregate);
        if (events?.subscribedEvents) {
            const projectionEntities = (aggregate.entities || []).filter((e: any) =>
                !e.isRoot && e.aggregateRef
            );

            const simpleSubscriptions = events.subscribedEvents.filter((sub: any) => {
                const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                    sub.conditions.some((c: any) => c.condition);
                const hasRouting = (sub as any).routingIdExpr;
                return !hasConditions && !hasRouting;
            });

            for (const sub of simpleSubscriptions) {
                const eventTypeName = (sub as any).eventType || '';
                if (!eventTypeName) continue;

                const isUpdate = eventTypeName.includes('Updated');
                const isDelete = eventTypeName.includes('Deleted');
                if (!isUpdate && !isDelete) continue;

                const publisherName = EventNameParser.extractEntityName(eventTypeName);

                let matched = projectionEntities.filter((e: any) =>
                    e.aggregateRef && e.aggregateRef.toLowerCase() === publisherName.toLowerCase()
                );
                if (matched.length === 0) {
                    matched = projectionEntities.filter((e: any) =>
                        e.aggregateRef && eventTypeName.toLowerCase().includes(e.aggregateRef.toLowerCase())
                    );
                }

                for (const entity of matched) {
                    if (isDelete) {
                        eventImports.add(`import ${eventsPackage}.${entity.name}DeletedEvent;`);
                    } else if (isUpdate) {
                        const entityHasLocalProps = ((entity as any).properties || []).some((prop: any) => {
                            const propName = (prop as any).name;
                            if (propName === 'id' || propName === 'aggregateId' ||
                                propName === 'version' || propName === 'state') {
                                return false;
                            }
                            const fieldMappings = (entity as any).fieldMappings || [];
                            const isFromMapping = fieldMappings.some((m: any) =>
                                m.entityField === propName || m.dtoField === propName
                            );
                            return !isFromMapping;
                        });

                        if (!entityHasLocalProps) {
                            eventImports.add(`import ${eventsPackage}.${entity.name}UpdatedEvent;`);
                        }
                    }
                }
            }
        }

        eventImports.forEach(imp => imports.push(imp));

        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', 'exception')}.${capitalize(projectName)}Exception;`);


        imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', lowerAggregate, 'coordination', 'webapi', 'requestDtos')}.Create${capitalize(aggregateName)}RequestDto;`);

        const preventRefs = findPreventReferencesTo(aggregateName);
        if (preventRefs.length > 0) {
            imports.push('import org.springframework.context.ApplicationContext;');
            for (const ref of preventRefs) {
                const sourceLower = ref.sourceAggregateName.toLowerCase();
                imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', sourceLower, 'aggregate')}.${ref.sourceAggregateName}Repository;`);
                imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', sourceLower, 'aggregate')}.${ref.sourceAggregateName};`);
            }
        }

        const enrichableSources = new Set<string>();
        for (const entity of aggregate.entities || []) {
            const aggRef = (entity as any).aggregateRef;
            if (!aggRef || aggRef === aggregateName) continue;
            const ultimateRoot = resolveUltimateSourceRoot(aggRef);
            if (ultimateRoot) {
                const ultimateName = (ultimateRoot.$container as any).name;
                if (ultimateName && ultimateName !== aggregateName) {
                    enrichableSources.add(ultimateName);
                }
            }
        }
        for (const sourceName of enrichableSources) {
            const sourceLower = sourceName.toLowerCase();
            imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'microservices', sourceLower, 'aggregate')}.${sourceName};`);
            imports.push(`import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${sourceName}Dto;`);
        }

        imports.push('');

        return imports.join('\n');
    }

    static generateClassDeclaration(aggregateName: string, projectName: string): string {
        const projectException = `${capitalize(projectName)}Exception`;
        return `@Service
@Transactional(noRollbackFor = ${projectException}.class)
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
            `    private UnitOfWorkService unitOfWorkService;`,
            '',
            `    @Autowired`,
            `    private ${capitalizedAggregate}Repository ${lowerAggregate}Repository;`,
            '',
            `    @Autowired`,
            `    private ${capitalizedAggregate}Factory ${lowerAggregate}Factory;`
        ];

        if (findPreventReferencesTo(aggregateName).length > 0) {
            dependencies.push(
                '',
                `    @Autowired`,
                `    private ApplicationContext applicationContext;`
            );
        }

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
