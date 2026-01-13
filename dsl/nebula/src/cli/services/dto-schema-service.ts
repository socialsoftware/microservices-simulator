import type { Aggregate, DtoFieldMapping, Entity, Model, Property } from "../../language/generated/ast.js";
import { getEntities } from "../utils/aggregate-helpers.js";
import { UnifiedTypeResolver, type ResolvedType } from "../generators/common/unified-type-resolver.js";

export interface DtoFieldSchema {
    name: string;
    javaType: string;
    isCollection: boolean;
    elementType?: string;
    sourceName?: string;
    sourceProperty?: Property;
    referencedEntityName?: string;
    referencedAggregateName?: string;
    referencedDtoName?: string;
    referencedEntityIsRoot?: boolean;
    referencedEntityHasGenerateDto?: boolean;
    requiresConversion?: boolean;
    extractField?: string;
    isAggregateField?: boolean;
    derivedAggregateId?: boolean;
    isEnum?: boolean;
    enumType?: string;
    isMappingOverride?: boolean;
    derivedAccessor?: string;
}

export interface DtoSchema {
    dtoName: string;
    entityName: string;
    aggregateName: string;
    fields: DtoFieldSchema[];
}

export interface DtoSchemaRegistry {
    dtos: DtoSchema[];
    dtoByName: Record<string, DtoSchema>;
    entityToDto: Record<string, DtoSchema>;
}

export class DtoSchemaService {
    buildFromModels(models: Model[]): DtoSchemaRegistry {
        const dtoSchemas: DtoSchema[] = [];
        const dtoByName: Record<string, DtoSchema> = {};
        const entityToDto: Record<string, DtoSchema> = {};

        const entityLookup = this.collectAllEntities(models);
        const dtoEnabledEntities = this.collectAllDtoEnabledEntities(models);

        // Collect all aggregates for cross-aggregate relationship detection
        const allAggregates: Aggregate[] = [];
        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                allAggregates.push(aggregate);
            }
        }

        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    // Generate DTOs for ALL entities in an aggregate:
                    // - Root entities always get a DTO
                    // - Non-root entities get their own DTO (with their own fields)
                    // This ensures we don't lose fields when non-root entities reference other aggregates

                    const dtoSchema = this.buildDtoSchemaForEntity(
                        entity,
                        aggregate,
                        entityLookup,
                        dtoEnabledEntities,
                        allAggregates
                    );
                    dtoSchemas.push(dtoSchema);
                    dtoByName[dtoSchema.dtoName] = dtoSchema;
                    entityToDto[dtoSchema.entityName] = dtoSchema;
                }
            }
        }

        return { dtos: dtoSchemas, dtoByName, entityToDto };
    }

    private buildDtoSchemaForEntity(
        entity: Entity,
        aggregate: Aggregate,
        entityLookup: Map<string, Entity>,
        dtoEnabledEntities: Map<string, Entity>,
        allAggregates?: Aggregate[]
    ): DtoSchema {
        const dtoName = `${entity.name}Dto`;
        const aggregateName = aggregate.name;

        const fields: DtoFieldSchema[] = [];
        const fieldMappings = this.createFieldMappingMap(entity as any);

        if (entity.isRoot) {
            fields.push(
                this.createStandardField('aggregateId', 'Integer'),
                this.createStandardField('version', 'Integer'),
                this.createStandardField('state', 'AggregateState')
            );
        }

        for (const property of entity.properties || []) {
            if (!property?.name) continue;
            if ((property as any).dtoExclude) {
                continue;
            }

            const mappingInfo = fieldMappings.get(property.name);
            const dtoFieldName = mappingInfo?.dtoField || property.name;
            const resolved = UnifiedTypeResolver.resolveDetailed(property.type, { targetContext: 'dto' });

            const fieldSchema = this.buildFieldSchemaFromProperty(
                dtoFieldName,
                property,
                resolved,
                entityLookup,
                dtoEnabledEntities,
                !!mappingInfo,
                aggregate,
                allAggregates
            );

            if (mappingInfo?.extractField) {
                fieldSchema.extractField = mappingInfo.extractField;
            }

            fields.push(fieldSchema);
        }

        return {
            dtoName,
            entityName: entity.name,
            aggregateName,
            fields,
        };
    }

    private buildFieldSchemaFromProperty(
        dtoFieldName: string,
        property: Property,
        resolved: ResolvedType,
        entityLookup: Map<string, Entity>,
        dtoEnabledEntities: Map<string, Entity>,
        isMappingOverride: boolean,
        currentAggregate?: Aggregate,
        allAggregates?: Aggregate[]
    ): DtoFieldSchema {
        let javaType = resolved.javaType;
        let referencedEntityName: string | undefined;
        let referencedAggregateName: string | undefined;
        let referencedDtoName: string | undefined;
        let requiresConversion = false;
        let elementType = resolved.elementType;
        const enumInfo = this.detectEnumProperty(property, resolved);

        if (enumInfo.isEnum) {
            if (resolved.isCollection) {
                const isSet = javaType.startsWith('Set<');
                const collectionType = isSet ? 'Set' : 'List';
                javaType = `${collectionType}<String>`;
                elementType = 'String';
            } else {
                javaType = 'String';
            }

            return {
                name: dtoFieldName,
                javaType,
                isCollection: resolved.isCollection,
                elementType,
                sourceName: property.name,
                sourceProperty: property,
                referencedEntityName,
                referencedAggregateName,
                referencedDtoName,
                requiresConversion,
                isEnum: true,
                enumType: enumInfo.enumType,
                isMappingOverride
            };
        }

        if (resolved.isCollection && resolved.elementType) {
            const elementEntity = this.lookupEntity(resolved.elementType, entityLookup, dtoEnabledEntities);
            if (elementEntity) {
                referencedEntityName = elementEntity.name;
                referencedAggregateName = elementEntity.$container?.name;

                // All entities within the same aggregate get their own DTOs now
                // So we always use the entity's own DTO (e.g., ExecutionUser -> ExecutionUserDto)
                const isSameAggregate = currentAggregate && referencedAggregateName === currentAggregate.name;

                if (isSameAggregate) {
                    // Same-aggregate: use the entity's own DTO
                    referencedDtoName = `${elementEntity.name}Dto`;
                    const elementTypeName = elementEntity.name;
                    javaType = javaType.replace(new RegExp(`\\b${elementTypeName}\\b`, 'g'), referencedDtoName);
                    elementType = referencedDtoName;
                    requiresConversion = true;
                } else {
                    // Cross-aggregate: use aggregateIds to avoid cyclic dependencies
                    const isSet = javaType.startsWith('Set<');
                    const collectionType = isSet ? 'Set' : 'List';
                    const aggregateIdFieldName = `${dtoFieldName}AggregateIds`;

                    // Determine the correct accessor from the element entity's dtoMapping
                    let derivedAccessor = 'getAggregateId';
                    const elementEntityAny = elementEntity as any;
                    if (elementEntityAny.dtoMapping?.fieldMappings) {
                        const aggIdMapping = elementEntityAny.dtoMapping.fieldMappings.find((fm: any) =>
                            fm.dtoField === 'aggregateId'
                        );
                        if (aggIdMapping && aggIdMapping.entityField) {
                            const capField = aggIdMapping.entityField.charAt(0).toUpperCase() + aggIdMapping.entityField.slice(1);
                            derivedAccessor = `get${capField}`;
                        }
                    }

                    return {
                        name: aggregateIdFieldName,
                        javaType: `${collectionType}<Integer>`,
                        isCollection: true,
                        elementType: 'Integer',
                        sourceName: property.name,
                        sourceProperty: property,
                        referencedEntityName,
                        referencedAggregateName,
                        derivedAggregateId: true,
                        isMappingOverride,
                        derivedAccessor
                    };
                }
            }
        } else if (resolved.isEntity) {
            const targetEntity = this.lookupEntity(resolved.javaType, entityLookup, dtoEnabledEntities);
            if (targetEntity) {
                referencedEntityName = targetEntity.name;
                referencedAggregateName = targetEntity.$container?.name;
            }

            // Check if this is same aggregate or cross-aggregate
            const isSameAggregate = currentAggregate && referencedAggregateName === currentAggregate.name;

            if (isSameAggregate && targetEntity) {
                // Same-aggregate: all entities get their own DTOs now
                referencedDtoName = `${targetEntity.name}Dto`;
                return {
                    name: dtoFieldName,
                    javaType: referencedDtoName,
                    isCollection: false,
                    sourceName: property.name,
                    sourceProperty: property,
                    referencedEntityName,
                    referencedAggregateName,
                    referencedDtoName,
                    referencedEntityIsRoot: targetEntity.isRoot || false,
                    referencedEntityHasGenerateDto: true, // All entities now get DTOs
                    requiresConversion: true,
                    isMappingOverride
                };
            }

            // Cross-aggregate: use aggregateId to maintain microservice boundaries
            // Determine the aggregateId field name based on the entity mapping
            let aggregateFieldName = `${dtoFieldName}AggregateId`;
            let derivedAccessor = 'getAggregateId';

            if (targetEntity && !targetEntity.isRoot) {
                // For non-root entities (like TopicCourse), check if they have a mapping that defines the aggregateId field
                const entityAny = targetEntity as any;
                const dtoMapping = entityAny.dtoMapping;
                if (dtoMapping?.fieldMappings) {
                    const aggIdMapping = dtoMapping.fieldMappings.find((fm: any) =>
                        fm.dtoField === 'aggregateId'
                    );
                    if (aggIdMapping && aggIdMapping.entityField) {
                        aggregateFieldName = aggIdMapping.entityField;
                        const capField = aggregateFieldName.charAt(0).toUpperCase() + aggregateFieldName.slice(1);
                        derivedAccessor = `get${capField}`;
                    } else {
                        const capField = aggregateFieldName.charAt(0).toUpperCase() + aggregateFieldName.slice(1);
                        derivedAccessor = `get${capField}`;
                    }
                } else {
                    const capField = aggregateFieldName.charAt(0).toUpperCase() + aggregateFieldName.slice(1);
                    derivedAccessor = `get${capField}`;
                }
            }

            return {
                name: aggregateFieldName,
                javaType: 'Integer',
                isCollection: false,
                sourceName: property.name,
                sourceProperty: property,
                referencedEntityName,
                referencedAggregateName,
                derivedAggregateId: true,
                isMappingOverride,
                derivedAccessor
            };
        }

        let referencedEntityIsRoot: boolean | undefined;
        let referencedEntityHasGenerateDto: boolean | undefined;
        if (resolved.isCollection && resolved.elementType) {
            const elementEntity = this.lookupEntity(resolved.elementType, entityLookup, dtoEnabledEntities);
            if (elementEntity) {
                referencedEntityIsRoot = elementEntity.isRoot || false;
                referencedEntityHasGenerateDto = !!(elementEntity as any).generateDto;
            }
        } else if (resolved.isEntity) {
            const targetEntity = this.lookupEntity(resolved.javaType, entityLookup, dtoEnabledEntities);
            if (targetEntity) {
                referencedEntityIsRoot = targetEntity.isRoot || false;
                referencedEntityHasGenerateDto = !!(targetEntity as any).generateDto;
            }
        }

        return {
            name: dtoFieldName,
            javaType,
            isCollection: resolved.isCollection,
            elementType,
            sourceName: property.name,
            sourceProperty: property,
            referencedEntityName,
            referencedAggregateName,
            referencedDtoName,
            referencedEntityIsRoot,
            referencedEntityHasGenerateDto,
            requiresConversion,
            isEnum: false,
            enumType: undefined,
            isMappingOverride
        };
    }

    private createStandardField(name: string, javaType: string): DtoFieldSchema {
        return {
            name,
            javaType,
            isCollection: false,
            isAggregateField: true,
            requiresConversion: false,
        };
    }

    private createFieldMappingMap(entity: any): Map<string, { dtoField: string; extractField?: string }> {
        const map = new Map<string, { dtoField: string; extractField?: string }>();
        const mapping = entity?.dtoMapping?.fieldMappings as DtoFieldMapping[] | undefined;
        if (!mapping) {
            return map;
        }

        for (const fieldMap of mapping) {
            if (!fieldMap?.entityField || !fieldMap.dtoField) continue;
            map.set(fieldMap.entityField, {
                dtoField: fieldMap.dtoField,
                extractField: fieldMap.extractField,
            });
        }

        return map;
    }

    private collectAllEntities(models: Model[]): Map<string, Entity> {
        const map = new Map<string, Entity>();

        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    if (entity?.name && !map.has(entity.name)) {
                        map.set(entity.name, entity);
                    }
                }
            }
        }

        return map;
    }

    private collectAllDtoEnabledEntities(models: Model[]): Map<string, Entity> {
        const map = new Map<string, Entity>();

        // All entities now get DTOs
        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    if (entity?.name && !map.has(entity.name)) {
                        map.set(entity.name, entity);
                    }
                }
            }
        }

        return map;
    }

    private lookupEntity(
        entityName: string,
        entityLookup: Map<string, Entity>,
        rootEntities: Map<string, Entity>
    ): Entity | undefined {
        return entityLookup.get(entityName) || rootEntities.get(entityName);
    }

    private detectEnumProperty(property: Property, resolved: ResolvedType): { isEnum: boolean; enumType?: string } {
        const typeNode: any = property.type;

        if (typeNode && typeof typeNode === 'object' && typeNode.$type === 'EntityType' && typeNode.type) {
            const ref = typeNode.type.ref as any;
            if (ref && ref.$type === 'EnumDefinition' && ref.name) {
                return { isEnum: true, enumType: ref.name };
            }
            if (typeNode.type.$refText && UnifiedTypeResolver.isEnumType(typeNode.type.$refText)) {
                return { isEnum: true, enumType: typeNode.type.$refText };
            }
        }

        if (UnifiedTypeResolver.isEnumType(resolved.javaType)) {
            return { isEnum: true, enumType: resolved.javaType };
        }

        return { isEnum: false };
    }
}

