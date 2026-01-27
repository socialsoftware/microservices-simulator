import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { TypeResolver } from "../../../common/resolvers/type-resolver.js";

interface EntityRelationship {
    entityType: string;
    paramName: string;
    javaType: string;
    isCollection: boolean;
}

export class ServiceCrudGenerator {
    static generateCrudMethods(aggregateName: string, rootEntity: Entity, projectName: string, aggregate?: Aggregate): string {
        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityName = rootEntity.name;

        // Find entity relationships for the create method
        const entityRelationships = aggregate ? this.findEntityRelationships(rootEntity, aggregate) : [];
        const singleEntityRels = entityRelationships.filter(rel => !rel.isCollection);
        const collectionEntityRels = entityRelationships.filter(rel => rel.isCollection);

        // Build create method parameters: projection entities + CreateRequestDto + UnitOfWork
        const createParams = this.generateCreateMethodParams(rootEntity, aggregateName, singleEntityRels, collectionEntityRels);
        const createBody = this.generateCreateMethodBody(rootEntity, aggregateName, projectName, singleEntityRels, collectionEntityRels);

        return `    // CRUD Operations
    public ${rootEntityName}Dto create${capitalizedAggregate}(${createParams}) {
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
            .filter(prop => prop.name.toLowerCase() !== 'id')
            .map(prop => {
                const setterName = `set${capitalize(prop.name)}`;
                const getterName = this.getGetterMethodName(prop);
                const isBoolean = this.isBooleanProperty(prop);

                if (isBoolean) {
                    return `            ${lowerAggregate}.${setterName}(${lowerAggregate}Dto.${getterName}());`;
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
            return `is${capitalizedName}`;
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
     * Find entity relationships (both single and collection entity fields) from root entity properties
     */
    private static findEntityRelationships(rootEntity: Entity, aggregate: Aggregate): EntityRelationship[] {
        const relationships: EntityRelationship[] = [];

        if (!rootEntity.properties) {
            return relationships;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            // Check if this is an entity type (not enum)
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                // Resolve entity type
                const entityRef = (prop.type as any).type?.ref;
                let entityName: string;

                if (isCollection) {
                    // For collections, extract element type
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    entityName = entityRef?.name || javaType;
                }

                // Only include if it's an entity within this aggregate
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                const isEntityInAggregate = !!relatedEntity;

                // Include all entity relationships in the service method signature
                // Note: generateDto flag just means "generate a DTO class", not "exclude from signature"
                if (isEntityInAggregate) {
                    const paramName = prop.name;
                    relationships.push({
                        entityType: entityName,
                        paramName,
                        javaType: isCollection ? javaType : entityName,
                        isCollection
                    });
                }
            }
        }

        return relationships;
    }

    /**
     * Check if a type is an enum
     */
    private static isEnumType(type: any): boolean {
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
                return true;
            }
            if (type.type.ref && type.type.ref.$type === 'EnumDefinition') {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate parameters for create method: projection entities + CreateRequestDto + UnitOfWork
     */
    private static generateCreateMethodParams(
        rootEntity: Entity,
        aggregateName: string,
        singleEntityRels: EntityRelationship[],
        collectionEntityRels: EntityRelationship[]
    ): string {
        const params: string[] = [];
        const capitalizedAggregate = capitalize(aggregateName);

        // Add single entity relationships first (projection entities created by saga)
        for (const rel of singleEntityRels) {
            params.push(`${rel.entityType} ${rel.paramName}`);
        }

        // Add CreateRequestDto (contains primitive fields + cross-aggregate DTOs)
        params.push(`Create${capitalizedAggregate}RequestDto createRequest`);

        // Add collection entity relationships (projection entities created by saga)
        for (const rel of collectionEntityRels) {
            params.push(`${rel.javaType} ${rel.paramName}`);
        }

        // Add UnitOfWork last
        params.push('UnitOfWork unitOfWork');

        return params.join(', ');
    }

    /**
     * Generate body for create method
     * Converts CreateRequestDto to regular DTO, then uses factory to create entity
     */
    private static generateCreateMethodBody(
        rootEntity: Entity,
        aggregateName: string,
        projectName: string,
        singleEntityRels: EntityRelationship[],
        collectionEntityRels: EntityRelationship[]
    ): string {
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityName = rootEntity.name;
        const capitalizedAggregate = capitalize(aggregateName);

        // Get primitive properties from rootEntity (these come from createRequest)
        const primitiveProps = rootEntity.properties?.filter(prop => {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
            const isPrimitive = !isCollection && !isEntityType;
            return isPrimitive && prop.name.toLowerCase() !== 'id';
        }) || [];

        // Build DTO conversion statements
        const dtoSetters = primitiveProps.map(prop => {
            const capitalizedName = capitalize(prop.name);
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isEnum = this.isEnumType(prop.type) || javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);
            
            // For enum types, convert to string using .name()
            if (isEnum) {
                return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}() != null ? createRequest.get${capitalizedName}().name() : null);`;
            }
            return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}());`;
        }).join('\n');

        // Build factory create method arguments
        // Order: aggregateId, single entities, DTO, collection entities
        const factoryArgs: string[] = ['aggregateId'];
        
        // Add single entity relationships
        for (const rel of singleEntityRels) {
            factoryArgs.push(rel.paramName);
        }

        // Add regular DTO (not createRequest)
        factoryArgs.push(`${lowerAggregate}Dto`);

        // Add collection entity relationships
        for (const rel of collectionEntityRels) {
            factoryArgs.push(rel.paramName);
        }

        return `            // Convert CreateRequestDto to regular DTO
            ${rootEntityName}Dto ${lowerAggregate}Dto = new ${rootEntityName}Dto();
${dtoSetters}
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            ${rootEntityName} ${lowerAggregate} = ${lowerAggregate}Factory.create${capitalizedAggregate}(${factoryArgs.join(', ')});
            unitOfWorkService.registerChanged(${lowerAggregate}, unitOfWork);
            return ${lowerAggregate}Factory.create${rootEntityName}Dto(${lowerAggregate});`;
    }
}
