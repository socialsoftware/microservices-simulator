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
    referencedDtoName?: string;
    requiresConversion?: boolean;
    extractField?: string;
    isAggregateField?: boolean;
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
        const rootEntities = this.collectAllRootEntities(models);

        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    if (!entity.isRoot) continue;

                    const dtoSchema = this.buildDtoSchemaForRootEntity(entity, aggregate, entityLookup, rootEntities);
                    dtoSchemas.push(dtoSchema);
                    dtoByName[dtoSchema.dtoName] = dtoSchema;
                    entityToDto[dtoSchema.entityName] = dtoSchema;
                }
            }
        }

        return { dtos: dtoSchemas, dtoByName, entityToDto };
    }

    private buildDtoSchemaForRootEntity(
        rootEntity: Entity,
        aggregate: Aggregate,
        entityLookup: Map<string, Entity>,
        rootEntities: Map<string, Entity>
    ): DtoSchema {
        const dtoName = `${rootEntity.name}Dto`;
        const aggregateName = aggregate.name;

        const fields: DtoFieldSchema[] = [];
        const fieldMappings = this.createFieldMappingMap(rootEntity as any);

        // Standard aggregate metadata fields
        fields.push(
            this.createStandardField('aggregateId', 'Integer'),
            this.createStandardField('version', 'Integer'),
            this.createStandardField('state', 'AggregateState')
        );

        for (const property of rootEntity.properties || []) {
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
                rootEntities
            );

            if (mappingInfo?.extractField) {
                fieldSchema.extractField = mappingInfo.extractField;
            }

            fields.push(fieldSchema);
        }

        return {
            dtoName,
            entityName: rootEntity.name,
            aggregateName,
            fields,
        };
    }

    private buildFieldSchemaFromProperty(
        dtoFieldName: string,
        property: Property,
        resolved: ResolvedType,
        entityLookup: Map<string, Entity>,
        rootEntities: Map<string, Entity>
    ): DtoFieldSchema {
        let javaType = resolved.javaType;
        let referencedEntityName: string | undefined;
        let referencedDtoName: string | undefined;
        let requiresConversion = false;
        let elementType = resolved.elementType;

        if (resolved.isCollection && resolved.elementType) {
            const elementEntity = this.lookupEntity(resolved.elementType, entityLookup, rootEntities);
            if (elementEntity) {
                referencedEntityName = elementEntity.name;
                referencedDtoName = this.resolveEntityDtoName(elementEntity, rootEntities);
                if (referencedDtoName) {
                    javaType = javaType.replace(resolved.elementType, referencedDtoName);
                    elementType = referencedDtoName;
                    requiresConversion = true;
                }
            }
        } else if (resolved.isEntity) {
            const targetEntity = this.lookupEntity(resolved.javaType, entityLookup, rootEntities);
            if (targetEntity) {
                referencedEntityName = targetEntity.name;
                referencedDtoName = this.resolveEntityDtoName(targetEntity, rootEntities);
                if (referencedDtoName) {
                    javaType = referencedDtoName;
                    requiresConversion = true;
                }
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
            referencedDtoName,
            requiresConversion,
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

    private resolveEntityDtoName(entity: Entity, rootEntities: Map<string, Entity>): string | undefined {
        if (entity.isRoot) {
            return `${entity.name}Dto`;
        }

        const entityAny = entity as any;
        const dtoType = entityAny.dtoType;
        if (dtoType?.ref?.name) {
            return dtoType.ref.name;
        }
        if (dtoType?.$refText) {
            return dtoType.$refText;
        }

        if (rootEntities.has(entity.name)) {
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

    private collectAllRootEntities(models: Model[]): Map<string, Entity> {
        const map = new Map<string, Entity>();

        for (const model of models) {
            for (const aggregate of model.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    if (entity?.isRoot && entity.name && !map.has(entity.name)) {
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
}

