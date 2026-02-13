import { EntityExt, AggregateExt } from "../../../../types/ast-extensions.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { CrudHelpers } from "../../../common/crud-helpers.js";
import { EntityRelationshipExtractor } from "../crud/entity-relationship-extractor.js";

export class ServiceCrudGenerator {
    static generateCrudMethods(aggregateName: string, rootEntity: EntityExt, projectName: string, aggregate?: AggregateExt): string {
        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityName = rootEntity.name;

        // All CRUD operations use UnitOfWork so sagas can track changes/events
        const createParams = `Create${capitalizedAggregate}RequestDto createRequest, UnitOfWork unitOfWork`;
        const createBody = this.generateCreateMethodBody(rootEntity, aggregateName, projectName, aggregate);

        return `    public ${rootEntityName}Dto create${capitalizedAggregate}(${createParams}) {
        try {
${createBody}
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error creating ${lowerAggregate}: " + e.getMessage());
        }
    }

    public ${rootEntityName}Dto get${capitalizedAggregate}ById(Integer id, UnitOfWork unitOfWork) {
        try {
            ${rootEntityName} ${lowerAggregate} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return ${lowerAggregate}Factory.create${rootEntityName}Dto(${lowerAggregate});
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving ${lowerAggregate}: " + e.getMessage());
        }
    }

    public List<${rootEntityName}Dto> getAll${capitalizedAggregate}s(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = ${lowerAggregate}Repository.findAll().stream()
                .map(${rootEntityName}::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(${lowerAggregate}Factory::create${rootEntityName}Dto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving all ${lowerAggregate}s: " + e.getMessage());
        }
    }

    public ${rootEntityName}Dto update${capitalizedAggregate}(${rootEntityName}Dto ${lowerAggregate}Dto, UnitOfWork unitOfWork) {
        try {
            Integer id = ${lowerAggregate}Dto.getAggregateId();
            ${rootEntityName} old${capitalizedAggregate} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            ${rootEntityName} new${capitalizedAggregate} = ${lowerAggregate}Factory.create${rootEntityName}FromExisting(old${capitalizedAggregate});
${this.generateUpdateLogic(rootEntity, aggregateName, true)}

            unitOfWorkService.registerChanged(new${capitalizedAggregate}, unitOfWork);
            ${capitalizedAggregate}UpdatedEvent event = new ${capitalizedAggregate}UpdatedEvent(${this.generateUpdateEventArgs(
                rootEntity,
                aggregateName,
                true
            )});
            event.setPublisherAggregateVersion(new${capitalizedAggregate}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return ${lowerAggregate}Factory.create${rootEntityName}Dto(new${capitalizedAggregate});
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error updating ${lowerAggregate}: " + e.getMessage());
        }
    }

    public void delete${capitalizedAggregate}(Integer id, UnitOfWork unitOfWork) {
        try {
            ${rootEntityName} old${capitalizedAggregate} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            ${rootEntityName} new${capitalizedAggregate} = ${lowerAggregate}Factory.create${rootEntityName}FromExisting(old${capitalizedAggregate});
            new${capitalizedAggregate}.remove();
            unitOfWorkService.registerChanged(new${capitalizedAggregate}, unitOfWork);
            unitOfWorkService.registerEvent(new ${capitalizedAggregate}DeletedEvent(new${capitalizedAggregate}.getAggregateId()), unitOfWork);
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error deleting ${lowerAggregate}: " + e.getMessage());
        }
    }`;
    }

    private static generateUpdateLogic(rootEntity: EntityExt, aggregateName: string, useNewVersion: boolean = false): string {
        if (!rootEntity.properties) return '';

        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedAggregate = capitalize(aggregateName);
        const targetVar = useNewVersion ? `new${capitalizedAggregate}` : lowerAggregate;

        const updates = rootEntity.properties
            .filter(prop => {
                const propName = prop.name.toLowerCase();
                if (propName === 'id') return false;

                // Skip final fields - they can't be updated
                if ((prop as any).isFinal) return false;

                // Skip entity relationships - they shouldn't be updated directly from DTO
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
                const isEntityType = !CrudHelpers.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
                if (isCollection || isEntityType) return false;

                return true;
            })
            .map(prop => {
                const setterName = `set${capitalize(prop.name)}`;
                const getterName = this.getGetterMethodName(prop);
                const isBoolean = this.isBooleanProperty(prop);
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isEnum = CrudHelpers.isEnumType(prop.type) || javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);

                if (isBoolean) {
                    return `            ${targetVar}.${setterName}(${lowerAggregate}Dto.${getterName}());`;
                } else if (isEnum) {
                    // For enum types, convert String from DTO to enum using valueOf
                    return `            if (${lowerAggregate}Dto.${getterName}() != null) {
                ${targetVar}.${setterName}(${javaType}.valueOf(${lowerAggregate}Dto.${getterName}()));
            }`;
                } else {
                    return `            if (${lowerAggregate}Dto.${getterName}() != null) {
                ${targetVar}.${setterName}(${lowerAggregate}Dto.${getterName}());
            }`;
                }
            });

        return updates.join('\n');
    }

    /**
     * Build argument list for <Aggregate>UpdatedEvent constructor.
     * Convention: first argument is aggregateId, followed by all primitive (non-relationship)
     * updatable properties of the root entity, in declaration order.
     */
    private static generateUpdateEventArgs(rootEntity: EntityExt, aggregateName: string, useNewVersion: boolean = false): string {
        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedAggregate = capitalize(aggregateName);
        const targetVar = useNewVersion ? `new${capitalizedAggregate}` : lowerAggregate;

        const args: string[] = [];
        // Always pass aggregateId first
        args.push(`${targetVar}.getAggregateId()`);

        if (!rootEntity.properties) {
            return args.join(', ');
        }

        for (const prop of rootEntity.properties) {
            const propName = (prop as any).name?.toLowerCase?.() ?? '';
            if (propName === 'id') continue;

            if ((prop as any).isFinal) continue;

            const javaType = TypeResolver.resolveJavaType((prop as any).type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isEntityType =
                !CrudHelpers.isEnumType((prop as any).type) && TypeResolver.isEntityType(javaType);
            if (isCollection || isEntityType) continue;

            // Skip enum-like properties; current *UpdatedEvent classes usually
            // don't carry enum fields such as Role/Type/State
            const isEnum =
                CrudHelpers.isEnumType((prop as any).type) ||
                javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);
            if (isEnum) continue;

            const getterName = this.getGetterMethodName(prop as any);
            args.push(`${targetVar}.${getterName}()`);
        }

        return args.join(', ');
    }

    private static getGetterMethodName(property: any): string {
        const capitalizedName = capitalize(property.name);
        const isBoolean = this.isBooleanProperty(property);

        if (isBoolean) {
            return `get${capitalizedName}`;
        }
        return `get${capitalizedName}`;
    }

    private static isBooleanProperty(property: any): boolean {
        if (!property.type) return false;
        if (property.type.$type === 'PrimitiveType') {
            return property.type.typeName?.toLowerCase() === 'boolean';
        }

        if (typeof property.type === 'string') {
            return property.type.toLowerCase() === 'boolean';
        }

        return false;
    }


    /**
     * SIMPLIFIED: Generate body for create method
     * Converts CreateRequestDto to regular DTO (including nested entity DTOs),
     * then uses factory to create entity with just (aggregateId, dto)
     */
    private static generateCreateMethodBody(
        rootEntity: EntityExt,
        aggregateName: string,
        projectName: string,
        aggregate?: AggregateExt
    ): string {
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityName = rootEntity.name;
        const capitalizedAggregate = capitalize(aggregateName);

        const entityRelationships = aggregate ? EntityRelationshipExtractor.findEntityRelationshipsWithDetails(rootEntity, aggregate) : [];

        const primitiveProps = rootEntity.properties?.filter(prop => {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isEntityType = !CrudHelpers.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
            const isPrimitive = !isCollection && !isEntityType;
            return isPrimitive && prop.name.toLowerCase() !== 'id';
        }) || [];

        const primitiveSetters = primitiveProps.map(prop => {
            const capitalizedName = capitalize(prop.name);
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isEnum = CrudHelpers.isEnumType(prop.type) || javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);

            if (isEnum) {
                return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}() != null ? createRequest.get${capitalizedName}().name() : null);`;
            }
            return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}());`;
        }).join('\n');

        const entitySetters = entityRelationships.map(rel => {
            const capitalizedName = capitalize(rel.paramName);
            
            if (rel.aggregateRef) {
                const projectionDtoName = `${rel.entityName}Dto`;
                
                if (rel.isCollection) {
                    return `            if (createRequest.get${capitalizedName}() != null) {
                ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}().stream().map(srcDto -> {
                    ${projectionDtoName} projDto = new ${projectionDtoName}();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.to${rel.collectionType}()));
            }`;
                } else {
                    return `            if (createRequest.get${capitalizedName}() != null) {
                ${projectionDtoName} ${rel.paramName}Dto = new ${projectionDtoName}();
                ${rel.paramName}Dto.setAggregateId(createRequest.get${capitalizedName}().getAggregateId());
                ${rel.paramName}Dto.setVersion(createRequest.get${capitalizedName}().getVersion());
                ${rel.paramName}Dto.setState(createRequest.get${capitalizedName}().getState());
                ${lowerAggregate}Dto.set${capitalizedName}(${rel.paramName}Dto);
            }`;
                }
            } else {
                if (rel.isCollection) {
                    return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}());`;
                } else {
                    return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}());`;
                }
            }
        }).join('\n');

        const allSetters = [primitiveSetters, entitySetters].filter(s => s).join('\n');

        return `            ${rootEntityName}Dto ${lowerAggregate}Dto = new ${rootEntityName}Dto();
${allSetters}
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            ${rootEntityName} ${lowerAggregate} = ${lowerAggregate}Factory.create${capitalizedAggregate}(aggregateId, ${lowerAggregate}Dto);
            unitOfWorkService.registerChanged(${lowerAggregate}, unitOfWork);
            return ${lowerAggregate}Factory.create${rootEntityName}Dto(${lowerAggregate});`;
    }




}
