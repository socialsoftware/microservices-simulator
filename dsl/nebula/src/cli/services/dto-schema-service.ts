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

        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    const shouldGenerateDto = entity.isRoot || (entity as any).generateDto;
                    if (!shouldGenerateDto) continue;

                    const dtoSchema = this.buildDtoSchemaForEntity(
                        entity,
                        aggregate,
                        entityLookup,
                        dtoEnabledEntities
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
        dtoEnabledEntities: Map<string, Entity>
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
                !!mappingInfo
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
        isMappingOverride: boolean
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
                referencedDtoName = this.resolveEntityDtoName(elementEntity, dtoEnabledEntities);
                if (referencedDtoName) {
                    const elementTypeName = elementEntity.name;
                    javaType = javaType.replace(new RegExp(`\\b${elementTypeName}\\b`, 'g'), referencedDtoName);
                    elementType = referencedDtoName;
                    requiresConversion = true;
                }
            }
        } else if (resolved.isEntity) {
            const targetEntity = this.lookupEntity(resolved.javaType, entityLookup, dtoEnabledEntities);
            if (targetEntity) {
                referencedEntityName = targetEntity.name;
                referencedAggregateName = targetEntity.$container?.name;
                referencedDtoName = this.resolveEntityDtoName(targetEntity, dtoEnabledEntities);
            }

            if (referencedDtoName) {
                return {
                    name: dtoFieldName,
                    javaType: referencedDtoName,
                    isCollection: false,
                    sourceName: property.name,
                    sourceProperty: property,
                    referencedEntityName,
                    referencedAggregateName,
                    referencedDtoName,
                    referencedEntityIsRoot: targetEntity?.isRoot || false,
                    referencedEntityHasGenerateDto: !!(targetEntity as any)?.generateDto,
                    requiresConversion: true,
                    isMappingOverride
                };
            }

            const aggregateFieldName = `${dtoFieldName}AggregateId`;
            let derivedAccessor = 'getAggregateId';
            if (targetEntity && !targetEntity.isRoot) {
                const capField = aggregateFieldName.charAt(0).toUpperCase() + aggregateFieldName.slice(1);
                derivedAccessor = `get${capField}`;
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

    private resolveEntityDtoName(entity: Entity, dtoEnabledEntities: Map<string, Entity>): string | undefined {
        if (entity.isRoot || (entity as any).generateDto) {
            return `${entity.name}Dto`;
        }

        const entityAny = entity as any;
        const dtoType = entityAny.dtoType;
        if (dtoType) {
            if (dtoType.ref?.name) {
                return dtoType.ref.name;
            }
            if (dtoType.$refText) {
                return dtoType.$refText;
            }
            if (typeof dtoType === 'string') {
                return dtoType;
            }
        }

        if (entityAny.dtoMapping) {
            // This case is handled by checking dtoEnabledEntities below
        }

        if (dtoEnabledEntities.has(entity.name)) {
            return `${entity.name}Dto`;
        }

        return undefined;
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

        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    const shouldGenerateDto = entity?.isRoot || (entity as any)?.generateDto;
                    if (shouldGenerateDto && entity?.name && !map.has(entity.name)) {
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

