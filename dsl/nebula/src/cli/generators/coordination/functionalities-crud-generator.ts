import { Entity, Aggregate } from '../common/parsers/model-parser.js';
import { OrchestrationBase } from '../common/orchestration-base.js';
import { TypeResolver } from '../common/resolvers/type-resolver.js';

export class FunctionalitiesCrudGenerator extends OrchestrationBase {
    generateCrudMethods(aggregateName: string, lowerAggregate: string, rootEntity: Entity, aggregate: Aggregate, allAggregates?: Aggregate[]): any[] {
        const dtoType = `${aggregateName}Dto`;
        const createRequestDtoType = `Create${aggregateName}RequestDto`;
        const methods: any[] = [];

        // Find cross-aggregate relationships for create method
        // With the new approach, DTOs are passed directly in the CreateRequestDto,
        // so we don't need separate service calls to fetch them
        const crossAggregateRefs = this.findCrossAggregateReferences(rootEntity, aggregate, allAggregates);

        // For create method, use the new CreateRequestDto which contains full DTOs
        const createParameters: any[] = [
            { type: createRequestDtoType, name: `createRequest` }
        ];

        const createParamNames = createParameters.map(p => p.name);

        methods.push({
            name: `create${aggregateName}`,
            returnType: dtoType,
            parameters: createParameters,
            body: this.generateCrudMethodBody('create', aggregateName, lowerAggregate, dtoType, createParamNames, [], crossAggregateRefs),
            throwsException: false
        });

        methods.push({
            name: `get${aggregateName}ById`,
            returnType: dtoType,
            parameters: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            body: this.generateCrudMethodBody('getById', aggregateName, lowerAggregate, dtoType, [`${lowerAggregate}AggregateId`]),
            throwsException: false
        });

        methods.push({
            name: `update${aggregateName}`,
            returnType: dtoType,
            parameters: [
                { type: dtoType, name: `${lowerAggregate}Dto` }
            ],
            body: this.generateCrudMethodBody('update', aggregateName, lowerAggregate, dtoType, [`${lowerAggregate}Dto`]),
            throwsException: false
        });

        methods.push({
            name: `delete${aggregateName}`,
            returnType: 'void',
            parameters: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            body: this.generateCrudMethodBody('delete', aggregateName, lowerAggregate, 'void', [`${lowerAggregate}AggregateId`]),
            throwsException: false
        });

        const searchableProperties = this.getSearchableProperties(rootEntity);
        if (searchableProperties.length > 0) {
            const searchParams = searchableProperties.map(prop => ({
                type: prop.type,
                name: prop.name
            }));

            methods.push({
                name: `search${aggregateName}s`,
                returnType: `List<${dtoType}>`,
                parameters: searchParams,
                body: this.generateCrudMethodBody('search', aggregateName, lowerAggregate, `List<${dtoType}>`, searchableProperties.map(p => p.name)),
                throwsException: false
            });
        }

        return methods;
    }

    private getSearchableProperties(entity: Entity): { name: string; type: string }[] {
        if (!entity.properties) return [];

        const searchableTypes = ['String', 'Boolean'];
        const properties: { name: string; type: string }[] = [];

        for (const prop of entity.properties) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            let isEnum = false;
            if (propType && typeof propType === 'object' && propType.$type === 'EntityType' && propType.type) {
                const ref = propType.type.ref;
                if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                    isEnum = true;
                } else if (propType.type.$refText) {
                    const javaType = TypeResolver.resolveJavaType(prop.type);
                    if (!TypeResolver.isPrimitiveType(javaType) && !TypeResolver.isEntityType(javaType) &&
                        !javaType.startsWith('List<') && !javaType.startsWith('Set<')) {
                        isEnum = true;
                    }
                }
            }

            if (searchableTypes.includes(typeName) || isEnum) {
                let javaType = TypeResolver.resolveJavaType(prop.type);
                if (javaType === 'boolean') {
                    javaType = 'Boolean';
                }
                properties.push({
                    name: prop.name,
                    type: javaType
                });
            }
        }

        for (const prop of entity.properties) {
            const typeNode: any = (prop as any).type;
            if (!typeNode || typeNode.$type !== 'EntityType' || !typeNode.type) continue;

            const refEntity = typeNode.type.ref as any;
            if (!refEntity || !refEntity.properties) continue;

            for (const relProp of refEntity.properties as any[]) {
                if (!relProp.name || !relProp.name.endsWith('AggregateId')) continue;

                const relType = relProp.type;
                const relTypeName = relType?.typeName || relType?.type?.$refText || relType?.$refText || '';
                if (relTypeName !== 'Integer' && relTypeName !== 'Long') continue;

                properties.push({
                    name: relProp.name,
                    type: relTypeName
                });
            }
        }

        return properties;
    }

    /**
     * Find entity relationships (both single and collection entity fields) from root entity properties
     */
    findEntityRelationships(rootEntity: Entity, aggregate: Aggregate): Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> {
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

                // Include all entity relationships
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
     * Get the related DTO type for an entity relationship
     */
    getRelatedDtoType(rel: { entityType: string; paramName: string; javaType: string; isCollection: boolean }, aggregate: Aggregate, allAggregates?: Aggregate[]): { dtoType: string | null; isFromAnotherAggregate: boolean; relatedAggregateName?: string } {
        const relatedEntity = aggregate.entities?.find((e: any) => e.name === rel.entityType);
        if (!relatedEntity) return { dtoType: null, isFromAnotherAggregate: false };

        const entityAny = relatedEntity as any;

        // Check if the entity uses an aggregate reference (from "uses Topic")
        const aggregateRef = entityAny.aggregateRef;
        let dtoTypeName: string | null = null;
        let relatedAggregateName: string | undefined = undefined;

        if (aggregateRef) {
            // aggregateRef is the aggregate name (e.g., "Topic"), derive DTO name (e.g., "TopicDto")
            if (typeof aggregateRef === 'string') {
                relatedAggregateName = aggregateRef;
                dtoTypeName = `${aggregateRef}Dto`;
            } else if (aggregateRef.ref?.name) {
                relatedAggregateName = aggregateRef.ref.name;
                dtoTypeName = `${aggregateRef.ref.name}Dto`;
            } else if (aggregateRef.$refText) {
                relatedAggregateName = aggregateRef.$refText;
                dtoTypeName = `${aggregateRef.$refText}Dto`;
            }
        }

        // If entity has generateDto, return the entity name + Dto
        if (!dtoTypeName && entityAny.generateDto) {
            dtoTypeName = `${rel.entityType}Dto`;
        }

        // Check if the DTO is from another aggregate
        if (relatedAggregateName && allAggregates) {
            const targetAggregate = allAggregates.find(agg => agg.name === relatedAggregateName);
            if (targetAggregate && targetAggregate.name !== aggregate.name) {
                return {
                    dtoType: dtoTypeName,
                    isFromAnotherAggregate: true,
                    relatedAggregateName: targetAggregate.name
                };
            }
        }

        return {
            dtoType: dtoTypeName,
            isFromAnotherAggregate: false
        };
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
            const ref = type.type.ref;
            if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                return true;
            }
        }
        return false;
    }

    /**
     * Find cross-aggregate references from the root entity's properties.
     * These are non-root entities that have "uses AnotherAggregate" declarations.
     */
    findCrossAggregateReferences(rootEntity: Entity, aggregate: Aggregate, allAggregates?: Aggregate[]): Array<{ entityType: string; paramName: string; relatedAggregate: string; relatedDtoType: string; isCollection: boolean }> {
        const references: Array<{ entityType: string; paramName: string; relatedAggregate: string; relatedDtoType: string; isCollection: boolean }> = [];

        if (!rootEntity.properties) {
            return references;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            // Check if this is an entity type
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                const entityRef = (prop.type as any).type?.ref;
                let entityName: string;

                if (isCollection) {
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    entityName = entityRef?.name || javaType;
                }

                // Find the entity in this aggregate
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                if (!relatedEntity) continue;

                // Check if the entity has aggregateRef (uses another aggregate)
                const entityAny = relatedEntity as any;
                const aggregateRef = entityAny.aggregateRef;

                if (aggregateRef) {
                    let relatedAggregateName: string | undefined;
                    if (typeof aggregateRef === 'string') {
                        relatedAggregateName = aggregateRef;
                    } else if (aggregateRef.ref?.name) {
                        relatedAggregateName = aggregateRef.ref.name;
                    } else if (aggregateRef.$refText) {
                        relatedAggregateName = aggregateRef.$refText;
                    }

                    if (relatedAggregateName) {
                        const isFromAnotherAggregate = allAggregates?.some(
                            agg => agg.name === relatedAggregateName && agg.name !== aggregate.name
                        );

                        if (isFromAnotherAggregate) {
                            references.push({
                                entityType: entityName,
                                paramName: prop.name,
                                relatedAggregate: relatedAggregateName,
                                relatedDtoType: `${relatedAggregateName}Dto`,
                                isCollection
                            });
                        }
                    }
                }
            }
        }

        return references;
    }

    private generateCrudMethodBody(operation: string, aggregateName: string, lowerAggregate: string, returnType: string, paramNames: string[], crossAggregateServices: Array<{ serviceName: string; aggregateName: string }> = [], crossAggregateRefs?: Array<{ entityType: string; paramName: string; relatedAggregate: string; relatedDtoType: string; isCollection: boolean }>): string {
        const capitalizedMethodName = this.capitalize(operation === 'getById' ? `get${aggregateName}ById` : operation === 'getAll' ? `getAll${aggregateName}s` : operation === 'search' ? `search${aggregateName}s` : `${operation}${aggregateName}`);
        const methodName = operation === 'getById' ? `get${aggregateName}ById` : operation === 'getAll' ? `getAll${aggregateName}s` : operation === 'search' ? `search${aggregateName}s` : `${operation}${aggregateName}`;
        const uncapitalizedMethodName = methodName.charAt(0).toLowerCase() + methodName.slice(1);

        // Build saga constructor params
        // Global order (for all operations):
        //  1) sagaUnitOfWork
        //  2) sagaUnitOfWorkService
        //  3) aggregate service
        //  4) method-specific parameters (CreateRequestDto, ids, etc.)
        // Note: With the new approach, we don't need cross-aggregate services for create operations
        // because the full DTOs are passed directly in the CreateRequestDto
        const sagaParams: string[] = ['sagaUnitOfWork', 'sagaUnitOfWorkService', `${lowerAggregate}Service`];

        // Then all method parameters
        sagaParams.push(...paramNames);

        const sagaParamsString = sagaParams.join(', ');

        let sagaReturn: string;
        if (returnType === 'void') {
            sagaReturn = 'break;';
        } else if (operation === 'create') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getCreated${aggregateName}Dto();`;
        } else if (operation === 'getAll') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.get${aggregateName}s();`;
        } else if (operation === 'search') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getSearched${aggregateName}Dtos();`;
        } else if (operation === 'getById') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.get${aggregateName}Dto();`;
        } else if (operation === 'update') {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getUpdated${aggregateName}Dto();`;
        } else {
            sagaReturn = `return ${uncapitalizedMethodName}FunctionalitySagas.getResult();`;
        }

        // Generate checkInput call for create and update operations
        // For create, use 'createRequest' as the param name
        const inputParamName = operation === 'create' ? 'createRequest' : (paramNames.find((p: string) => p.endsWith('Dto')) || `${lowerAggregate}Dto`);
        const checkInputCall = (operation === 'create' || operation === 'update')
            ? `checkInput(${inputParamName});\n                `
            : '';

        return `String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ${checkInputCall}${capitalizedMethodName}FunctionalitySagas ${uncapitalizedMethodName}FunctionalitySagas = new ${capitalizedMethodName}FunctionalitySagas(
                        ${sagaParamsString});
                ${uncapitalizedMethodName}FunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                ${sagaReturn}
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }`;
    }
}


