import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { TypeResolver } from "../../../common/resolvers/type-resolver.js";

export class ServiceCollectionGenerator {
    /**
     * Generate all collection manipulation methods for an aggregate
     */
    static generateCollectionMethods(aggregateName: string, rootEntity: Entity, projectName: string, aggregate?: Aggregate): string {
        if (!rootEntity.properties || !aggregate) {
            return '';
        }

        const methods: string[] = [];
        const collections = this.findCollectionProperties(rootEntity, aggregate);

        for (const collection of collections) {
            // Generate 5 methods per collection
            methods.push(this.generateAddMethod(collection, aggregateName, rootEntity, projectName));
            methods.push(this.generateAddBatchMethod(collection, aggregateName, rootEntity, projectName));
            methods.push(this.generateGetMethod(collection, aggregateName, rootEntity, projectName));
            methods.push(this.generateRemoveMethod(collection, aggregateName, rootEntity, projectName));
            methods.push(this.generateUpdateMethod(collection, aggregateName, rootEntity, projectName, aggregate));
        }

        return methods.join('\n\n');
    }

    private static findCollectionProperties(rootEntity: Entity, aggregate: Aggregate): Array<{
        propertyName: string;
        elementType: string;
        collectionType: 'Set' | 'List';
        isProjection: boolean;
        identifierField: string;
        singularName: string;
        capitalizedSingular: string;
        capitalizedCollection: string;
    }> {
        const collections: any[] = [];

        if (!rootEntity.properties) {
            return collections;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isSet = javaType.startsWith('Set<');
            const isList = javaType.startsWith('List<');

            if (isSet || isList) {
                const elementType = TypeResolver.getElementType(prop.type);
                if (elementType && TypeResolver.isEntityType(javaType)) {
                    const elementEntity = aggregate.entities?.find((e: any) => e.name === elementType);
                    if (!elementEntity) continue;

                    const isProjection = (elementEntity as any).aggregateRef !== undefined;
                    const identifierField = isProjection
                        ? this.buildAggregateIdFieldName(elementType)
                        : this.determineBusinessKey(elementEntity);

                    const singularName = this.singularize(elementType);

                    collections.push({
                        propertyName: prop.name,
                        elementType,
                        collectionType: isSet ? 'Set' : 'List',
                        isProjection,
                        identifierField,
                        singularName,
                        capitalizedSingular: capitalize(singularName),
                        capitalizedCollection: capitalize(prop.name)
                    });
                }
            }
        }

        return collections;
    }

    private static buildAggregateIdFieldName(entityName: string): string {
        const referencedName = this.extractReferencedAggregateName(entityName);
        return `${referencedName.toLowerCase()}AggregateId`;
    }

    private static extractReferencedAggregateName(entityName: string): string {
        for (let i = entityName.length - 1; i >= 0; i--) {
            if (entityName[i] === entityName[i].toUpperCase() && i > 0) {
                const candidate = entityName.substring(i);
                if (candidate.length > 1) {
                    return candidate;
                }
            }
        }
        return entityName;
    }

    private static determineBusinessKey(entity: any): string {
        if (!entity || !entity.properties) {
            return 'key';
        }

        const commonKeys = ['key', 'code', 'id', 'sequence'];
        for (const keyName of commonKeys) {
            const field = entity.properties.find((p: any) => p.name === keyName);
            if (field) {
                return keyName;
            }
        }

        const firstIntField = entity.properties.find((p: any) => {
            const javaType = TypeResolver.resolveJavaType(p.type);
            return javaType === 'Integer' &&
                !p.name.endsWith('AggregateId') &&
                !p.name.endsWith('Version');
        });

        return firstIntField?.name || 'key';
    }

    private static singularize(word: string): string {
        if (word.endsWith('s')) {
            return word.slice(0, -1);
        }
        return word;
    }

    private static generateAddMethod(collection: any, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        return `    public ${collection.elementType}Dto add${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${collection.elementType} element = new ${collection.elementType}(${collection.singularName}Dto);
            ${lowerEntity}.get${collection.capitalizedCollection}().add(element);
            unitOfWorkService.registerChanged(${lowerEntity}, unitOfWork);
            return ${collection.singularName}Dto;
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error adding ${collection.singularName}: " + e.getMessage());
        }
    }`;
    }

    private static generateAddBatchMethod(collection: any, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        return `    public List<${collection.elementType}Dto> add${collection.capitalizedSingular}s(Integer ${lowerEntity}Id, List<${collection.elementType}Dto> ${collection.singularName}Dtos, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${collection.singularName}Dtos.forEach(dto -> {
                ${collection.elementType} element = new ${collection.elementType}(dto);
                ${lowerEntity}.get${collection.capitalizedCollection}().add(element);
            });
            unitOfWorkService.registerChanged(${lowerEntity}, unitOfWork);
            return ${collection.singularName}Dtos;
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error adding ${collection.singularName}s: " + e.getMessage());
        }
    }`;
    }

    private static generateGetMethod(collection: any, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const capitalizedIdentifier = capitalize(collection.identifierField);

        return `    public ${collection.elementType}Dto get${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${collection.elementType} element = ${lowerEntity}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${collection.elementType} not found"));
            return element.buildDto();
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving ${collection.singularName}: " + e.getMessage());
        }
    }`;
    }

    private static generateRemoveMethod(collection: any, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const capitalizedIdentifier = capitalize(collection.identifierField);

        return `    public void remove${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${lowerEntity}.get${collection.capitalizedCollection}().removeIf(item ->
                item.get${capitalizedIdentifier}() != null &&
                item.get${capitalizedIdentifier}().equals(${collection.identifierField})
            );
            unitOfWorkService.registerChanged(${lowerEntity}, unitOfWork);
            ${collection.elementType}RemovedEvent event = new ${collection.elementType}RemovedEvent(${lowerEntity}Id, ${collection.identifierField});
            event.setPublisherAggregateVersion(${lowerEntity}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error removing ${collection.singularName}: " + e.getMessage());
        }
    }`;
    }

    private static generateUpdateMethod(collection: any, aggregateName: string, rootEntity: Entity, projectName: string, aggregate: Aggregate): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const capitalizedIdentifier = capitalize(collection.identifierField);

        // Get updatable fields from the element entity
        const elementEntity = aggregate.entities?.find((e: any) => e.name === collection.elementType);
        const updatableFieldsWithMapping = this.extractUpdatableFieldsWithMapping(elementEntity, collection.isProjection, collection.identifierField);

        const updateFieldsCode = updatableFieldsWithMapping.map(field => {
            // Use DTO field name for getter (local name from mapping)
            const dtoGetterField = capitalize(field.dtoFieldName);
            // Use entity field name for setter (prefixed name in entity)
            const entitySetterField = capitalize(field.entityFieldName);

            return `            if (${collection.singularName}Dto.get${dtoGetterField}() != null) {
                element.set${entitySetterField}(${collection.singularName}Dto.get${dtoGetterField}());
            }`;
        }).join('\n');

        // Build event constructor parameters
        // For projection entities, events include all primitive fields
        // For non-projection entities, events only include identifier
        let eventConstructorParams: string;
        if (collection.isProjection && elementEntity) {
            // Projection entity: pass all fields (aggregateId, identifier, version, + all primitive fields)
            const allEventParams = this.buildProjectionEventParameters(
                elementEntity,
                lowerEntity,
                collection.identifierField,
                'element'
            );
            eventConstructorParams = allEventParams;
        } else {
            // Non-projection entity: only aggregateId + identifier
            eventConstructorParams = `${lowerEntity}Id, ${collection.identifierField}`;
        }

        return `    public ${collection.elementType}Dto update${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${collection.elementType} element = ${lowerEntity}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${collection.elementType} not found"));
${updateFieldsCode}
            unitOfWorkService.registerChanged(${lowerEntity}, unitOfWork);
            ${collection.elementType}UpdatedEvent event = new ${collection.elementType}UpdatedEvent(${eventConstructorParams});
            event.setPublisherAggregateVersion(${lowerEntity}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error updating ${collection.singularName}: " + e.getMessage());
        }
    }`;
    }

    /**
     * Extract updatable fields with both DTO and entity field names for projection entities.
     * This is necessary because DTOs use local names (from dtoField) while entities use prefixed names (entityField).
     */
    private static extractUpdatableFieldsWithMapping(entity: any, isProjection: boolean, identifierField: string): Array<{
        entityFieldName: string;
        dtoFieldName: string;
        type: string;
    }> {
        if (!entity || !entity.properties) {
            return [];
        }

        const fields: Array<{ entityFieldName: string; dtoFieldName: string; type: string }> = [];
        const excludedFields = ['aggregateId', 'version', 'state', identifierField];

        // For projection entities with field mappings
        if (isProjection && entity.fieldMappings) {
            const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'boolean', 'LocalDateTime', 'Double', 'Float', 'double', 'float', 'int', 'long'];

            for (const mapping of entity.fieldMappings) {
                const entityFieldName = mapping.entityField; // e.g., "userName"
                const dtoFieldName = mapping.dtoField;       // e.g., "name"

                // Skip system fields
                if (entityFieldName.endsWith('AggregateId') ||
                    entityFieldName.endsWith('Version') ||
                    entityFieldName.endsWith('State') ||
                    excludedFields.includes(entityFieldName)) {
                    continue;
                }

                const javaType = TypeResolver.resolveJavaType(mapping.type);

                // Skip collections and entity references
                if (javaType.startsWith('Set<') || javaType.startsWith('List<') ||
                    TypeResolver.isEntityType(javaType)) {
                    continue;
                }

                // Include primitive mapped fields
                if (primitiveTypes.some(t => javaType.includes(t))) {
                    fields.push({
                        entityFieldName: entityFieldName,
                        dtoFieldName: dtoFieldName,
                        type: javaType
                    });
                }
            }
        } else {
            // For non-projection entities, entity field name = DTO field name
            for (const prop of entity.properties) {
                const propName = prop.name;

                // Skip excluded fields
                if (excludedFields.includes(propName)) continue;

                // Skip final fields
                if (prop.isFinal) continue;

                // Get the Java type
                const javaType = TypeResolver.resolveJavaType(prop.type);

                // Skip collection fields
                if (javaType.startsWith('Set<') || javaType.startsWith('List<')) {
                    continue;
                }

                // Skip entity references
                if (TypeResolver.isEntityType(javaType)) {
                    continue;
                }

                fields.push({
                    entityFieldName: propName,
                    dtoFieldName: propName, // Same for non-projection entities
                    type: javaType
                });
            }
        }

        return fields;
    }

    /**
     * Build event constructor parameters for projection entity UpdatedEvents.
     * These events include all primitive fields from the projection entity.
     *
     * Parameter order matches the event constructor:
     * 1. aggregateId (root aggregate ID)
     * 2. {prefix}AggregateId (referenced aggregate ID)
     * 3. {prefix}Version (referenced aggregate version)
     * 4. All other primitive mapped fields
     *
     * @param entity The projection entity
     * @param aggregateVarName Variable name of the root aggregate (e.g., "answer")
     * @param identifierField The identifier field name (e.g., "questionAggregateId")
     * @param elementVarName Variable name of the element object (e.g., "element")
     * @returns Comma-separated parameter list for event constructor
     */
    private static buildProjectionEventParameters(
        entity: any,
        aggregateVarName: string,
        identifierField: string,
        elementVarName: string
    ): string {
        const params: string[] = [];

        // 1. aggregateId (root aggregate)
        params.push(`${aggregateVarName}Id`);

        // 2. {prefix}AggregateId (referenced aggregate ID - the identifier)
        params.push(`${elementVarName}.get${capitalize(identifierField)}()`);

        // 3. {prefix}Version (referenced aggregate version)
        // Extract prefix from identifierField (e.g., "questionAggregateId" -> "question")
        const prefix = identifierField.replace(/AggregateId$/, '');
        const versionField = `${prefix}Version`;
        params.push(`${elementVarName}.get${capitalize(versionField)}()`);

        // 4. All other primitive mapped fields (in order from fieldMappings)
        const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'boolean', 'LocalDateTime', 'Double', 'Float', 'double', 'float', 'int', 'long'];
        const fieldMappings = entity.fieldMappings || [];

        for (const mapping of fieldMappings) {
            const fieldName = mapping.entityField;

            // Skip system fields (already included above)
            if (fieldName === 'id' ||
                fieldName.endsWith('AggregateId') ||
                fieldName.endsWith('Version') ||
                fieldName.endsWith('State')) {
                continue;
            }

            // Get Java type
            const javaType = TypeResolver.resolveJavaType(mapping.type);

            // Skip collections and entity references
            if (javaType.startsWith('Set<') ||
                javaType.startsWith('List<') ||
                TypeResolver.isEntityType(javaType)) {
                continue;
            }

            // Include primitive mapped fields
            if (primitiveTypes.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${capitalize(fieldName)}()`);
            }
        }

        // Also check regular properties (for explicitly declared fields)
        for (const prop of entity.properties || []) {
            const propName = prop.name;

            // Skip system fields
            if (propName === 'id' ||
                propName.endsWith('AggregateId') ||
                propName.endsWith('Version') ||
                propName.endsWith('State')) {
                continue;
            }

            // Get Java type
            const javaType = TypeResolver.resolveJavaType(prop.type);

            // Skip collections and entity references
            if (javaType.startsWith('Set<') ||
                javaType.startsWith('List<') ||
                TypeResolver.isEntityType(javaType)) {
                continue;
            }

            // Include primitive fields (avoid duplicates from fieldMappings)
            const alreadyIncluded = fieldMappings.some((m: any) => m.entityField === propName);
            if (!alreadyIncluded && primitiveTypes.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${capitalize(propName)}()`);
            }
        }

        return params.join(', ');
    }
}
