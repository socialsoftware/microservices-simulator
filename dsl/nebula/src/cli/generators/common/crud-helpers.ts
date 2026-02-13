import { StringUtils } from "../../utils/string-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "./unified-type-resolver.js";

/**
 * Shared CRUD generation helpers to avoid duplication across service, coordination, and saga generators
 */
export class CrudHelpers {
    /**
     * Generate standardized CRUD method names
     */
    static getMethodNames(aggregateName: string) {
        const capitalized = StringUtils.capitalize(aggregateName);
        return {
            create: `create${capitalized}`,
            getById: `get${capitalized}ById`,
            getAll: `getAll${capitalized}s`,
            update: `update${capitalized}`,
            delete: `delete${capitalized}`
        };
    }

    /**
     * Generate standardized DTO type names
     */
    static getDtoTypes(aggregateName: string) {
        const capitalized = StringUtils.capitalize(aggregateName);
        return {
            dto: `${capitalized}Dto`,
            createRequest: `Create${capitalized}RequestDto`,
            updateRequest: `Update${capitalized}RequestDto`
        };
    }

    /**
     * Generate standardized event names
     */
    static getEventNames(aggregateName: string) {
        const capitalized = StringUtils.capitalize(aggregateName);
        return {
            created: `${capitalized}CreatedEvent`,
            updated: `${capitalized}UpdatedEvent`,
            deleted: `${capitalized}DeletedEvent`
        };
    }

    /**
     * Find cross-aggregate references in an entity
     * Returns entities that reference other aggregates
     */
    static findCrossAggregateReferences(
        rootEntity: any,
        aggregate: any,
        allAggregates?: any[]
    ): Array<{
        entityType: string;
        paramName: string;
        relatedAggregate: string;
        relatedDtoType: string;
        isCollection: boolean;
    }> {
        const references: Array<{
            entityType: string;
            paramName: string;
            relatedAggregate: string;
            relatedDtoType: string;
            isCollection: boolean;
        }> = [];

        if (!rootEntity?.properties || !allAggregates) {
            return references;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            // Check if this property is an entity type
            const isEntityType = TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                let entityName: string;

                if (isCollection) {
                    entityName = TypeResolver.getElementType(prop.type) ||
                                javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    const entityRef = (prop.type as any).type?.ref;
                    entityName = entityRef?.name || javaType;
                }

                // Check if this entity is from a different aggregate (cross-aggregate reference)
                const localEntity = aggregate.entities?.find((e: any) => e.name === entityName);

                if (!localEntity) {
                    // This is a cross-aggregate reference
                    // Find which aggregate it belongs to
                    for (const otherAgg of allAggregates) {
                        if (otherAgg.name === aggregate.name) continue;

                        const externalEntity = otherAgg.entities?.find((e: any) => e.name === entityName);
                        if (externalEntity) {
                            const paramName = StringUtils.lowercase(prop.name);
                            const relatedAggregate = otherAgg.name;
                            const relatedDtoType = `${entityName}Dto`;

                            references.push({
                                entityType: entityName,
                                paramName,
                                relatedAggregate,
                                relatedDtoType,
                                isCollection
                            });
                            break;
                        }
                    }
                }
            }
        }

        return references;
    }

    /**
     * Find entity relationships within the same aggregate
     * Returns entities that are part of this aggregate (not cross-aggregate)
     */
    static findEntityRelationships(
        rootEntity: any,
        aggregate: any
    ): Array<{
        entityType: string;
        paramName: string;
        javaType: string;
        isCollection: boolean;
    }> {
        const relationships: Array<{
            entityType: string;
            paramName: string;
            javaType: string;
            isCollection: boolean;
        }> = [];

        if (!rootEntity?.properties) {
            return relationships;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            const isEntityType = TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                let entityName: string;

                if (isCollection) {
                    entityName = TypeResolver.getElementType(prop.type) ||
                                javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    const entityRef = (prop.type as any).type?.ref;
                    entityName = entityRef?.name || javaType;
                }

                // Check if this entity is within the same aggregate
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);

                if (relatedEntity) {
                    relationships.push({
                        entityType: entityName,
                        paramName: StringUtils.lowercase(prop.name),
                        javaType,
                        isCollection
                    });
                }
            }
        }

        return relationships;
    }

    /**
     * Generate parameter list for a CRUD method
     */
    static generateParameterList(operation: 'create' | 'getById' | 'update' | 'delete' | 'getAll', aggregateName: string): Array<{ type: string; name: string }> {
        const lower = aggregateName.toLowerCase();
        const dtoTypes = this.getDtoTypes(aggregateName);

        switch (operation) {
            case 'create':
                return [{ type: dtoTypes.createRequest, name: 'createRequest' }];

            case 'getById':
                return [{ type: 'Integer', name: `${lower}AggregateId` }];

            case 'update':
                return [{ type: dtoTypes.dto, name: `${lower}Dto` }];

            case 'delete':
                return [{ type: 'Integer', name: `${lower}AggregateId` }];

            case 'getAll':
                return [];

            default:
                return [];
        }
    }

    /**
     * Get return type for a CRUD method
     */
    static getReturnType(operation: 'create' | 'getById' | 'update' | 'delete' | 'getAll', aggregateName: string): string {
        const dtoTypes = this.getDtoTypes(aggregateName);

        switch (operation) {
            case 'create':
            case 'getById':
            case 'update':
                return dtoTypes.dto;

            case 'delete':
                return 'void';

            case 'getAll':
                return `List<${dtoTypes.dto}>`;

            default:
                return 'void';
        }
    }

    /**
     * Check if a property should be excluded from update operations
     */
    static shouldExcludeFromUpdate(propertyName: string): boolean {
        const lowerPropName = propertyName.toLowerCase();
        return lowerPropName === 'id' ||
               lowerPropName === 'aggregateid' ||
               lowerPropName === 'version' ||
               lowerPropName === 'state';
    }

    /**
     * Check if a type is an enum type
     * Based on original implementation from service/default/crud-generator.ts
     */
    static isEnumType(type: any): boolean {
        if (!type) return false;

        // Check if it's an EntityType with enum reference
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            // Check if reference text matches enum naming pattern
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/)) {
                return true;
            }
            // Check if it's explicitly an EnumDefinition
            if (type.type.ref && (type.type.ref.$type === 'EnumDefinition' || type.type.ref.$type === 'Enum')) {
                return true;
            }
        }

        return false;
    }
}
