import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { TypeResolver } from "../../../common/resolvers/type-resolver.js";
import { getEntities, getEvents } from "../../../../utils/aggregate-helpers.js";

export class ServiceCrudGenerator {
    static generateCrudMethods(aggregateName: string, rootEntity: Entity, projectName: string, aggregate?: Aggregate): string {
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
            ${rootEntityName} ${lowerAggregate} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
${this.generateUpdateLogic(rootEntity, aggregateName)}

            unitOfWorkService.registerChanged(${lowerAggregate}, unitOfWork);
            ${capitalizedAggregate}UpdatedEvent event = new ${capitalizedAggregate}UpdatedEvent(${this.generateUpdateEventArgs(
                rootEntity,
                aggregateName
            )});
            event.setPublisherAggregateVersion(${lowerAggregate}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return ${lowerAggregate}Factory.create${rootEntityName}Dto(${lowerAggregate});
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error updating ${lowerAggregate}: " + e.getMessage());
        }
    }

    public void delete${capitalizedAggregate}(Integer id, UnitOfWork unitOfWork) {
        try {
            ${rootEntityName} ${lowerAggregate} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            ${lowerAggregate}.remove();
            unitOfWorkService.registerChanged(${lowerAggregate}, unitOfWork);
            unitOfWorkService.registerEvent(new ${capitalizedAggregate}DeletedEvent(${lowerAggregate}.getAggregateId()), unitOfWork);
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

    /**
     * Build argument list for <Aggregate>UpdatedEvent constructor.
     * Convention: first argument is aggregateId, followed by all primitive (non-relationship)
     * updatable properties of the root entity, in declaration order.
     */
    private static generateUpdateEventArgs(rootEntity: Entity, aggregateName: string): string {
        const lowerAggregate = aggregateName.toLowerCase();

        const args: string[] = [];
        // Always pass aggregateId first
        args.push(`${lowerAggregate}.getAggregateId()`);

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
                !this.isEnumType((prop as any).type) && TypeResolver.isEntityType(javaType);
            if (isCollection || isEntityType) continue;

            // Skip enum-like properties; current *UpdatedEvent classes usually
            // don't carry enum fields such as Role/Type/State
            const isEnum =
                this.isEnumType((prop as any).type) ||
                javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);
            if (isEnum) continue;

            const getterName = this.getGetterMethodName(prop as any);
            args.push(`${lowerAggregate}.${getterName}()`);
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

    /**
     * Generate event handler methods for subscribed CRUD events.
     * These methods handle events by updating/marking as INACTIVE projections when events are received.
     */
    static generateProjectionMethods(aggregateName: string, aggregate: Aggregate, projectName: string): string {
        const events = getEvents(aggregate);
        if (!events || !events.subscribedEvents) {
            return '';
        }

        // Filter for simple subscriptions (Update/Delete events)
        const simpleSubscriptions = events.subscribedEvents.filter((sub: any) => {
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        if (simpleSubscriptions.length === 0) {
            return '';
        }

        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            return '';
        }

        const methods: string[] = [];

        for (const subscription of simpleSubscriptions) {
            const eventTypeName = (subscription as any).eventType || '';
            if (!eventTypeName) continue;

            const isUpdate = eventTypeName.includes('Updated');
            const isDelete = eventTypeName.includes('Deleted');

            if (!isUpdate && !isDelete) continue;

            // Extract publisher aggregate name (e.g., UserDeletedEvent -> User)
            const publisherAggregateName = eventTypeName.replace(/(Updated|Deleted|Created)Event$/, '');

            // Find ALL entities in this aggregate that use the publisher aggregate
            const entities = getEntities(aggregate);
            const projectionEntities = entities.filter((e: any) => {
                const aggregateRef = e.aggregateRef;
                return aggregateRef && aggregateRef.toLowerCase() === publisherAggregateName.toLowerCase();
            });

            // Always generate handler methods, even if no projection entities exist
            // If no projections, generates a stub handler for custom business logic
            // if (projectionEntities.length === 0) continue; // REMOVED: Now generate handlers for all subscribed events

            // Generate a single event handler method that handles all matching projections (or stub if none)
            if (isDelete) {
                methods.push(this.generateEventHandlerMethod(
                    capitalizedAggregate,
                    lowerAggregate,
                    rootEntity,
                    projectionEntities,
                    publisherAggregateName,
                    eventTypeName,
                    'delete',
                    projectName
                ));
            } else if (isUpdate) {
                methods.push(this.generateEventHandlerMethod(
                    capitalizedAggregate,
                    lowerAggregate,
                    rootEntity,
                    projectionEntities,
                    publisherAggregateName,
                    eventTypeName,
                    'update',
                    projectName
                ));
            }
        }

        return methods.length > 0 ? '\n' + methods.join('\n\n') : '';
    }

    /**
     * Extract primitive field parameters needed for projection entity UpdatedEvent
     * Returns both method signature params and constructor call params
     */
    private static extractPrimitiveFieldsForEvent(projectionEntities: Entity[], publisherAggregateName: string): {
        methodSignature: string;
        paramList: string[];
    } {
        if (projectionEntities.length === 0) {
            return { methodSignature: '', paramList: [] };
        }

        // Use the first projection entity to extract field mappings
        const projectionEntity = projectionEntities[0];
        const fieldMappings = (projectionEntity as any).fieldMappings || [];

        const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'boolean', 'LocalDateTime', 'Double', 'Float', 'double', 'float', 'int', 'long'];
        const fields: Array<{ javaType: string; paramName: string }> = [];

        // ONLY extract from fieldMappings (mapped properties from referenced aggregate)
        // DO NOT extract local properties - those don't exist in the incoming event!
        for (const mapping of fieldMappings) {
            const entityField = mapping.entityField;
            const dtoField = mapping.dtoField;

            // Skip system fields
            if (entityField.endsWith('AggregateId') || entityField.endsWith('Version') || entityField.endsWith('State')) {
                continue;
            }

            const javaType = TypeResolver.resolveJavaType(mapping.type);

            // Only include primitive fields (use === for exact match or includes for partial match)
            if (primitiveTypes.some(t => javaType === t || javaType.includes(t))) {
                // Use the DTO field name as the parameter name (camelCase)
                fields.push({
                    javaType,
                    paramName: dtoField
                });
            }
        }

        // Build method signature: ", Type param1, Type param2"
        const methodSignature = fields.length > 0
            ? ', ' + fields.map(f => `${f.javaType} ${f.paramName}`).join(', ')
            : '';

        // Build param list for constructor call: ["param1", "param2"]
        const paramList = fields.map(f => f.paramName);

        return { methodSignature, paramList };
    }

    /**
     * Generate a single event handler method that handles all matching projections
     */
    private static generateEventHandlerMethod(
        capitalizedAggregate: string,
        lowerAggregate: string,
        rootEntity: Entity,
        projectionEntities: Entity[],
        publisherAggregateName: string,
        eventTypeName: string,
        action: 'delete' | 'update',
        projectName: string
    ): string {
        const rootEntityName = rootEntity.name;
        const methodName = `handle${eventTypeName}`;

        // Build the method body
        const projectionUpdates: string[] = [];

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;

            // Extract the field prefix from the projection entity
            // Look for the aggregateId field to determine the prefix
            // E.g., TournamentCreator has "creatorAggregateId" -> prefix is "creator"
            let fieldPrefix = publisherAggregateName.toLowerCase();
            const aggregateIdField = (projectionEntity.properties || []).find((p: any) =>
                p.name && p.name.endsWith('AggregateId')
            );
            if (aggregateIdField) {
                // Extract prefix from field like "creatorAggregateId" -> "creator"
                fieldPrefix = (aggregateIdField as any).name.replace(/AggregateId$/, '');
            }
            const capitalizedFieldPrefix = capitalize(fieldPrefix);

            // Find properties in root entity containing this projection
            const matchingProperties = rootEntity.properties?.filter(prop => {
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const elementType = TypeResolver.getElementType(prop.type);
                return javaType.includes(projectionEntityName) || elementType === projectionEntityName;
            }) || [];

            for (const prop of matchingProperties) {
                const isCollection = TypeResolver.resolveJavaType(prop.type).startsWith('Set<') ||
                                   TypeResolver.resolveJavaType(prop.type).startsWith('List<');
                const propName = prop.name;

                if (isCollection) {
                    // For collections: find all matching entities and mark as INACTIVE
                    if (action === 'delete') {
                        projectionUpdates.push(
                            `        // Handle ${propName} collection\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().stream()\n` +
                            `                .filter(item -> item.get${capitalizedFieldPrefix}AggregateId() != null && \n` +
                            `                               item.get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId))\n` +
                            `                .forEach(item -> item.set${capitalizedFieldPrefix}State(Aggregate.AggregateState.INACTIVE));\n` +
                            `        }`
                        );
                    } else if (action === 'update') {
                        projectionUpdates.push(
                            `        // Handle ${propName} collection\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().stream()\n` +
                            `                .filter(item -> item.get${capitalizedFieldPrefix}AggregateId() != null && \n` +
                            `                               item.get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId))\n` +
                            `                .forEach(item -> item.set${capitalizedFieldPrefix}Version(${publisherAggregateName.toLowerCase()}Version));\n` +
                            `        }`
                        );
                    }
                } else {
                    // For single entity: check and update/mark as INACTIVE
                    if (action === 'delete') {
                        projectionUpdates.push(
                            `        // Handle ${propName} single reference\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null && \n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId() != null &&\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId)) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().set${capitalizedFieldPrefix}State(Aggregate.AggregateState.INACTIVE);\n` +
                            `        }`
                        );
                    } else if (action === 'update') {
                        projectionUpdates.push(
                            `        // Handle ${propName} single reference\n` +
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null && \n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId() != null &&\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId)) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().set${capitalizedFieldPrefix}Version(${publisherAggregateName.toLowerCase()}Version);\n` +
                            `        }`
                        );
                    }
                }
            }
        }

        const projectionUpdateCode = projectionUpdates.join('\n\n');

        // Generate event registrations for projection entities
        const eventRegistrations: string[] = [];

        // Extract primitive fields from the publisher aggregate's UpdatedEvent
        // We need to pass these to the projection entity UpdatedEvent
        const primitiveFieldParams = this.extractPrimitiveFieldsForEvent(projectionEntities, publisherAggregateName);

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;

            if (action === 'delete') {
                const prefix = publisherAggregateName.toLowerCase();
                // Register DeletedEvent for projection entity
                eventRegistrations.push(
                    `        unitOfWorkService.registerEvent(\n` +
                    `            new ${projectionEntityName}DeletedEvent(\n` +
                    `                new${rootEntityName}.getAggregateId(),\n` +
                    `                ${prefix}AggregateId\n` +
                    `            ),\n` +
                    `            unitOfWork\n` +
                    `        );`
                );
            } else if (action === 'update') {
                // Check if projection entity has local properties (properties not mapped from source aggregate)
                const hasLocalProperties = (projectionEntity.properties || []).some((prop: any) => {
                    const propName = prop.name;
                    // Skip system fields
                    if (propName === 'id' || propName.endsWith('AggregateId') ||
                        propName.endsWith('Version') || propName.endsWith('State')) {
                        return false;
                    }
                    // Check if this property is from fieldMappings
                    const fieldMappings = (projectionEntity as any).fieldMappings || [];
                    const isFromMapping = fieldMappings.some((m: any) => m.entityField === propName || m.dtoField === propName);
                    return !isFromMapping; // Has local property if not from mapping
                });

                // Only register UpdatedEvent if projection entity has NO local properties
                // (because we can't populate local properties from the incoming event)
                if (!hasLocalProperties) {
                    const prefix = publisherAggregateName.toLowerCase();
                    // Build the parameter list for the UpdatedEvent constructor
                    // Format: aggregateId, {prefix}AggregateId, {prefix}Version, ...primitive fields
                    const eventParams = [
                        `new${rootEntityName}.getAggregateId()`,
                        `${prefix}AggregateId`,
                        `${prefix}Version`,
                        ...primitiveFieldParams.paramList
                    ].join(',\n                    ');

                    eventRegistrations.push(
                        `        unitOfWorkService.registerEvent(\n` +
                        `            new ${projectionEntityName}UpdatedEvent(\n` +
                        `                    ${eventParams}\n` +
                        `            ),\n` +
                        `            unitOfWork\n` +
                        `        );`
                    );
                }
            }
        }

        const eventRegistrationCode = eventRegistrations.length > 0
            ? '\n' + eventRegistrations.join('\n\n')
            : '';

        // Build method parameter list including primitive fields for update action
        const methodParamList = action === 'update'
            ? `Integer aggregateId, Integer ${publisherAggregateName.toLowerCase()}AggregateId, Integer ${publisherAggregateName.toLowerCase()}Version${primitiveFieldParams.methodSignature}`
            : `Integer aggregateId, Integer ${publisherAggregateName.toLowerCase()}AggregateId, Integer ${publisherAggregateName.toLowerCase()}Version`;

        return `    public ${rootEntityName} ${methodName}(${methodParamList}, UnitOfWork unitOfWork) {
        try {
            ${rootEntityName} old${rootEntityName} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            ${rootEntityName} new${rootEntityName} = ${lowerAggregate}Factory.create${rootEntityName}FromExisting(old${rootEntityName});

${projectionUpdateCode}

            unitOfWorkService.registerChanged(new${rootEntityName}, unitOfWork);
${eventRegistrationCode}

            return new${rootEntityName};
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error handling ${eventTypeName}: " + e.getMessage());
        }
    }`;
    }

}
