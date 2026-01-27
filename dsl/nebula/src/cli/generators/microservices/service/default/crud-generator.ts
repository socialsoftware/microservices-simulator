import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { TypeResolver } from "../../../common/resolvers/type-resolver.js";

export class ServiceCrudGenerator {
    static generateCrudMethods(aggregateName: string, rootEntity: Entity, projectName: string, aggregate?: Aggregate): string {
        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityName = rootEntity.name;

        const createParams = `Create${capitalizedAggregate}RequestDto createRequest, UnitOfWork unitOfWork`;
        const createBody = this.generateCreateMethodBody(rootEntity, aggregateName, projectName, aggregate);

        return `    public ${rootEntityName}Dto create${capitalizedAggregate}(${createParams}) {
        try {
${createBody}
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error creating ${lowerAggregate}: " + e.getMessage());
        }
    }

    public ${rootEntityName}Dto get${capitalizedAggregate}ById(Integer id) {
        try {
            ${rootEntityName} ${lowerAggregate} = (${rootEntityName}) ${lowerAggregate}Repository.findById(id)
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id));
            return new ${rootEntityName}Dto(${lowerAggregate});
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving ${lowerAggregate}: " + e.getMessage());
        }
    }

    public List<${rootEntityName}Dto> getAll${capitalizedAggregate}s() {
        try {
            return ${lowerAggregate}Repository.findAll().stream()
                .map(entity -> new ${rootEntityName}Dto((${rootEntityName}) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving all ${lowerAggregate}s: " + e.getMessage());
        }
    }

    public ${rootEntityName}Dto update${capitalizedAggregate}(${rootEntityName}Dto ${lowerAggregate}Dto) {
        try {
            Integer id = ${lowerAggregate}Dto.getAggregateId();
            ${rootEntityName} ${lowerAggregate} = (${rootEntityName}) ${lowerAggregate}Repository.findById(id)
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id));
            
            ${this.generateUpdateLogic(rootEntity, aggregateName)}
            
            ${lowerAggregate} = ${lowerAggregate}Repository.save(${lowerAggregate});
            return new ${rootEntityName}Dto(${lowerAggregate});
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error updating ${lowerAggregate}: " + e.getMessage());
        }
    }

    public void delete${capitalizedAggregate}(Integer id) {
        try {
            if (!${lowerAggregate}Repository.existsById(id)) {
                throw new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id);
            }
            ${lowerAggregate}Repository.deleteById(id);
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error deleting ${lowerAggregate}: " + e.getMessage());
        }
    }`;
    }

    private static generateUpdateLogic(rootEntity: Entity, aggregateName: string): string {
        if (!rootEntity.properties) return '';

        const lowerAggregate = aggregateName.toLowerCase();
        const updates = rootEntity.properties
            .filter(prop => {
                const propName = prop.name.toLowerCase();
                if (propName === 'id') return false;
                
                // Skip final fields - they can't be updated
                if ((prop as any).isFinal) return false;
                
                // Skip entity relationships - they shouldn't be updated directly from DTO
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
                const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
                if (isCollection || isEntityType) return false;
                
                return true;
            })
            .map(prop => {
                const setterName = `set${capitalize(prop.name)}`;
                const getterName = this.getGetterMethodName(prop);
                const isBoolean = this.isBooleanProperty(prop);
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isEnum = this.isEnumType(prop.type) || javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);

                if (isBoolean) {
                    return `            ${lowerAggregate}.${setterName}(${lowerAggregate}Dto.${getterName}());`;
                } else if (isEnum) {
                    // For enum types, convert String from DTO to enum using valueOf
                    return `            if (${lowerAggregate}Dto.${getterName}() != null) {
                ${lowerAggregate}.${setterName}(${javaType}.valueOf(${lowerAggregate}Dto.${getterName}()));
            }`;
                } else {
                    return `            if (${lowerAggregate}Dto.${getterName}() != null) {
                ${lowerAggregate}.${setterName}(${lowerAggregate}Dto.${getterName}());
            }`;
                }
            });

        return updates.join('\n');
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
     * Check if a type is an enum
     */
    private static isEnumType(type: any): boolean {
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/)) {
                return true;
            }
            if (type.type.ref && type.type.ref.$type === 'EnumDefinition') {
                return true;
            }
        }
        return false;
    }

    /**
     * SIMPLIFIED: Generate body for create method
     * Converts CreateRequestDto to regular DTO (including nested entity DTOs),
     * then uses factory to create entity with just (aggregateId, dto)
     */
    private static generateCreateMethodBody(
        rootEntity: Entity,
        aggregateName: string,
        projectName: string,
        aggregate?: Aggregate
    ): string {
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityName = rootEntity.name;
        const capitalizedAggregate = capitalize(aggregateName);

        const entityRelationships = aggregate ? this.findEntityRelationshipsWithDetails(rootEntity, aggregate) : [];

        const primitiveProps = rootEntity.properties?.filter(prop => {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
            const isPrimitive = !isCollection && !isEntityType;
            return isPrimitive && prop.name.toLowerCase() !== 'id';
        }) || [];

        const primitiveSetters = primitiveProps.map(prop => {
            const capitalizedName = capitalize(prop.name);
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isEnum = this.isEnumType(prop.type) || javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);
            
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

    /**
     * Find entity relationships with full details including aggregateRef
     */
    private static findEntityRelationshipsWithDetails(
        rootEntity: Entity, 
        aggregate: Aggregate
    ): Array<{ 
        paramName: string; 
        entityName: string;
        isCollection: boolean; 
        collectionType: string;
        aggregateRef: string | null;
    }> {
        const relationships: Array<{ 
            paramName: string; 
            entityName: string;
            isCollection: boolean; 
            collectionType: string;
            aggregateRef: string | null;
        }> = [];

        if (!rootEntity.properties) {
            return relationships;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const collectionType = javaType.startsWith('Set<') ? 'Set' : 'List';

            // Check if this is an entity type (not enum)
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                // Get element type for collections
                let entityName: string;
                if (isCollection) {
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    const entityRef = (prop.type as any).type?.ref;
                    entityName = entityRef?.name || javaType;
                }

                // Find the related entity to check for aggregateRef
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                if (relatedEntity) {
                    // Check if this entity has an aggregateRef (references another aggregate)
                    const aggregateRef = (relatedEntity as any).aggregateRef as string | undefined;
                    
                    relationships.push({
                        paramName: prop.name,
                        entityName: entityName,
                        isCollection,
                        collectionType,
                        aggregateRef: aggregateRef || null
                    });
                }
            }
        }

        return relationships;
    }
}
