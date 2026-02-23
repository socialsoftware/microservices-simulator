import { StringUtils } from "../../utils/string-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "./unified-type-resolver.js";



export class CrudHelpers {


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



    static getDtoTypes(aggregateName: string) {
        const capitalized = StringUtils.capitalize(aggregateName);
        return {
            dto: `${capitalized}Dto`,
            createRequest: `Create${capitalized}RequestDto`,
            updateRequest: `Update${capitalized}RequestDto`
        };
    }



    static getEventNames(aggregateName: string) {
        const capitalized = StringUtils.capitalize(aggregateName);
        return {
            created: `${capitalized}CreatedEvent`,
            updated: `${capitalized}UpdatedEvent`,
            deleted: `${capitalized}DeletedEvent`
        };
    }



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


                const localEntity = aggregate.entities?.find((e: any) => e.name === entityName);

                if (!localEntity) {


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



    static shouldExcludeFromUpdate(propertyName: string): boolean {
        const lowerPropName = propertyName.toLowerCase();
        return lowerPropName === 'id' ||
            lowerPropName === 'aggregateid' ||
            lowerPropName === 'version' ||
            lowerPropName === 'state';
    }



    static isEnumType(type: any): boolean {
        if (!type) return false;

        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            if (type.type.ref && (type.type.ref.$type === 'EnumDefinition' || type.type.ref.$type === 'Enum')) {
                return true;
            }
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*(Type|State|Role|Status|Category|Method|Kind|Mode|Level|Priority)$/)) {
                return true;
            }
        }

        return false;
    }
}
