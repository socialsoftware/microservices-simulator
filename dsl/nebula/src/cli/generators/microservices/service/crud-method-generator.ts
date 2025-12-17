import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "../../common/unified-type-resolver.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";

export interface GeneratedMethod {
    name: string;
    parameters: MethodParameter[];
    returnType: string;
    annotations: string[];
    implementation?: any[];
}

export interface MethodParameter {
    type: string;
    name: string;
}

export interface CrudGenerationOptions {
    includeCreate: boolean;
    includeRead: boolean;
    includeUpdate: boolean;
    includeDelete: boolean;
    includeFindAll: boolean;
    includeCustomQueries: boolean;
    transactional: boolean;
    includeValidation: boolean;
}

export class CrudMethodGenerator {

    generateCrudMethods(
        aggregate: Aggregate,
        rootEntity: Entity,
        options: CrudGenerationOptions = this.getDefaultOptions()
    ): GeneratedMethod[] {
        const methods: GeneratedMethod[] = [];
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        const properties = this.extractSimpleProperties(rootEntity);

        if (options.includeCreate) {
            methods.push(this.generateCreateMethod(aggregate, rootEntity, entityName, lowerEntity, options, properties));
        }

        if (options.includeRead) {
            methods.push(this.generateFindByIdMethod(entityName, options));
        }

        if (options.includeUpdate) {
            methods.push(this.generateUpdateMethod(entityName, lowerEntity, options, properties));
        }

        if (options.includeDelete) {
            methods.push(this.generateDeleteMethod(entityName, options));
        }

        const searchableProperties = this.getSearchableProperties(aggregate, rootEntity);
        if (searchableProperties.length > 0) {
            methods.push(this.generateSearchMethod(aggregateName, entityName, options, searchableProperties));
        } else if (options.includeFindAll) {
            methods.push(this.generateFindAllMethod(aggregateName, entityName, options));
        }

        return methods;
    }

    private extractSimpleProperties(entity: Entity): { name: string; capitalizedName: string; isFinal?: boolean }[] {
        const simpleTypes = ['String', 'Integer', 'Long', 'Boolean', 'Double', 'Float', 'LocalDateTime', 'LocalDate', 'BigDecimal'];
        const properties: { name: string; capitalizedName: string; isFinal?: boolean }[] = [];

        for (const prop of entity.properties || []) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            if (simpleTypes.includes(typeName)) {
                const propName = prop.name;
                properties.push({
                    name: propName,
                    capitalizedName: propName.charAt(0).toUpperCase() + propName.slice(1),
                    isFinal: (prop as any).isFinal || false
                });
            }
        }

        return properties;
    }

    private generateCreateMethod(aggregate: Aggregate, rootEntity: Entity, entityName: string, lowerEntity: string, options: CrudGenerationOptions, properties: { name: string; capitalizedName: string }[]): GeneratedMethod {
        // Find entity relationships (both single and collections)
        const entityRelationships = this.findEntityRelationships(rootEntity, aggregate);
        const singleEntityRels = entityRelationships.filter(rel => !rel.isCollection);
        const collectionEntityRels = entityRelationships.filter(rel => rel.isCollection);

        // Build parameters: single entities, DTO, collections, UnitOfWork
        const parameters: MethodParameter[] = [];

        // Add single entity relationships first
        for (const rel of singleEntityRels) {
            parameters.push({ type: rel.entityType, name: rel.paramName });
        }

        // Add DTO
        parameters.push({ type: `${entityName}Dto`, name: `${lowerEntity}Dto` });

        // Add collection entity relationships
        for (const rel of collectionEntityRels) {
            parameters.push({ type: rel.javaType, name: rel.paramName });
        }

        // Add UnitOfWork last
        parameters.push({ type: 'UnitOfWork', name: 'unitOfWork' });

        return {
            name: `create${entityName}`,
            parameters,
            returnType: `${entityName}Dto`,
            annotations: [],
            crudAction: 'create',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`,
            properties,
            entityRelationships: {
                single: singleEntityRels,
                collections: collectionEntityRels
            }
        } as any;
    }

    /**
     * Find entity relationships (both single and collection entity fields) from root entity properties
     */
    private findEntityRelationships(rootEntity: Entity, aggregate: Aggregate): Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> {
        const relationships: Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> = [];

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

                // Exclude DTO entities (entities marked with 'Dto' keyword)
                const isDtoEntity = relatedEntity && (relatedEntity as any).generateDto;

                if (isEntityInAggregate && !isDtoEntity) {
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
    private isEnumType(type: any): boolean {
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

    private generateFindByIdMethod(entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        return {
            name: `get${entityName}ById`,
            parameters: [
                { type: 'Integer', name: 'aggregateId' },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: `${entityName}Dto`,
            annotations: [],
            crudAction: 'findById',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`
        } as any;
    }

    private generateUpdateMethod(entityName: string, lowerEntity: string, options: CrudGenerationOptions, properties: { name: string; capitalizedName: string; isFinal?: boolean }[]): GeneratedMethod {
        const nonFinalProperties = properties.filter(p => !p.isFinal);

        return {
            name: `update${entityName}`,
            parameters: [
                { type: 'Integer', name: 'aggregateId' },
                { type: `${entityName}Dto`, name: `${lowerEntity}Dto` },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: `${entityName}Dto`,
            annotations: [],
            crudAction: 'update',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`,
            properties,
            nonFinalProperties
        } as any;
    }

    private generateDeleteMethod(entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        return {
            name: `delete${entityName}`,
            parameters: [
                { type: 'Integer', name: 'aggregateId' },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: 'void',
            annotations: [],
            crudAction: 'delete',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`
        } as any;
    }

    private generateFindAllMethod(aggregateName: string, entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        return {
            name: `getAll${aggregateName}s`,
            parameters: [
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: `List<${entityName}Dto>`,
            annotations: [],
            crudAction: 'findAll',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`
        } as any;
    }

    private generateSearchMethod(aggregateName: string, entityName: string, options: CrudGenerationOptions, searchableProperties: { name: string; type: string }[]): GeneratedMethod {
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);
        const parameters = searchableProperties.map(prop => ({
            type: prop.type,
            name: prop.name
        }));
        parameters.push({ type: 'UnitOfWork', name: 'unitOfWork' });

        return {
            name: `search${aggregateName}s`,
            parameters,
            returnType: `List<${entityName}Dto>`,
            annotations: [],
            crudAction: 'search',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`,
            searchableProperties
        } as any;
    }

    private getSearchableProperties(aggregate: Aggregate, entity: Entity): { name: string; type: string; accessor: string }[] {
        if (!entity.properties) return [];

        const searchableTypes = ['String', 'Boolean'];
        const properties: { name: string; type: string; accessor: string }[] = [];

        for (const prop of entity.properties) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            let isEnum = false;
            if (propType && typeof propType === 'object' && propType.$type === 'EntityType' && propType.type) {
                const ref = propType.type.ref as any;
                if (ref && ref.$type === 'EnumDefinition' && ref.name) {
                    isEnum = true;
                } else if (propType.type.$refText && UnifiedTypeResolver.isEnumType(propType.type.$refText)) {
                    isEnum = true;
                }
            }

            if (searchableTypes.includes(typeName) || isEnum) {
                const javaType = typeName === 'String' ? 'String' : typeName === 'Boolean' ? 'Boolean' :
                    isEnum ? (propType?.type?.$refText || typeName) : typeName;
                const capName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                const accessor = `entity.get${capName}()`;
                properties.push({
                    name: prop.name,
                    type: javaType,
                    accessor
                });
            }
        }

        for (const prop of entity.properties) {
            const typeNode: any = (prop as any).type;
            if (!typeNode || typeNode.$type !== 'EntityType' || !typeNode.type) continue;

            const refEntity = typeNode.type.ref as Entity | undefined;
            if (!refEntity || !refEntity.properties) continue;

            const relationName = prop.name;
            const capRelation = relationName.charAt(0).toUpperCase() + relationName.slice(1);

            for (const relProp of refEntity.properties as any[]) {
                if (!relProp.name || !relProp.name.endsWith('AggregateId')) continue;

                const relType = relProp.type;
                const relTypeName = relType?.typeName || relType?.type?.$refText || relType?.$refText || '';
                if (relTypeName !== 'Integer' && relTypeName !== 'Long') continue;

                const capRelField = relProp.name.charAt(0).toUpperCase() + relProp.name.slice(1);
                const accessor = `entity.get${capRelation}().get${capRelField}()`;

                properties.push({
                    name: relProp.name,
                    type: relTypeName,
                    accessor
                });
            }
        }

        return properties;
    }

    private getDefaultOptions(): CrudGenerationOptions {
        return {
            includeCreate: true,
            includeRead: true,
            includeUpdate: true,
            includeDelete: true,
            includeFindAll: true,
            includeCustomQueries: false,
            transactional: true,
            includeValidation: true
        };
    }

    static createOptions(overrides: Partial<CrudGenerationOptions> = {}): CrudGenerationOptions {
        const defaults: CrudGenerationOptions = {
            includeCreate: true,
            includeRead: true,
            includeUpdate: true,
            includeDelete: true,
            includeFindAll: true,
            includeCustomQueries: false,
            transactional: true,
            includeValidation: true
        };

        return { ...defaults, ...overrides };
    }
}
