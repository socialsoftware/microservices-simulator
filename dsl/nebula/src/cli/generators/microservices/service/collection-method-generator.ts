import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../common/unified-type-resolver.js";
import { capitalize } from "../../../utils/string-utils.js";
import { GeneratedMethod, MethodParameter } from "./crud-method-generator.js";

export interface CollectionInfo {
    propertyName: string;           
    elementType: string;            
    isProjection: boolean;          
    identifierField: string;        
    identifierType: string;         
    collectionType: 'Set' | 'List';
    singularName: string;           
    capitalizedSingular: string;    
    capitalizedCollection: string;  
}

export interface CollectionMethod extends GeneratedMethod {
    collectionInfo: CollectionInfo;
    operation: 'add' | 'addBatch' | 'get' | 'remove' | 'update';
}

export class CollectionMethodGenerator {

    

    generateCollectionMethods(aggregate: Aggregate, rootEntity: Entity): CollectionMethod[] {
        const methods: CollectionMethod[] = [];
        const collections = this.findCollectionProperties(rootEntity);

        for (const collection of collections) {
            const collectionInfo = this.buildCollectionInfo(collection, aggregate);

            
            methods.push(this.generateAddMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateAddBatchMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateGetMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateRemoveMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateUpdateMethod(collectionInfo, aggregate, rootEntity));
        }

        return methods;
    }

    

    private findCollectionProperties(entity: Entity): Array<{ name: string; elementType: string; type: 'Set' | 'List' }> {
        const collections: Array<{ name: string; elementType: string; type: 'Set' | 'List' }> = [];

        if (!entity.properties) {
            return collections;
        }

        for (const prop of entity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isSet = javaType.startsWith('Set<');
            const isList = javaType.startsWith('List<');

            if (isSet || isList) {
                const elementType = TypeResolver.getElementType(prop.type);
                if (elementType && TypeResolver.isEntityType(javaType)) {
                    collections.push({
                        name: prop.name,
                        elementType,
                        type: isSet ? 'Set' : 'List'
                    });
                }
            }
        }

        return collections;
    }

    

    private buildCollectionInfo(
        collection: { name: string; elementType: string; type: 'Set' | 'List' },
        aggregate: Aggregate
    ): CollectionInfo {
        const elementEntity = this.findEntityByName(aggregate, collection.elementType);
        const isProjection = this.hasAggregateRef(elementEntity);

        
        const identifierField = isProjection
            ? this.buildAggregateIdFieldName(collection.elementType)
            : this.determineBusinessKey(elementEntity);

        const singularName = this.singularize(collection.elementType);

        return {
            propertyName: collection.name,
            elementType: collection.elementType,
            isProjection,
            identifierField,
            identifierType: 'Integer',
            collectionType: collection.type,
            singularName,
            capitalizedSingular: capitalize(singularName),
            capitalizedCollection: capitalize(collection.name)
        };
    }

    

    private hasAggregateRef(entity: Entity | null): boolean {
        if (!entity) return false;
        return (entity as any).aggregateRef !== undefined && (entity as any).aggregateRef !== null;
    }

    

    private findEntityByName(aggregate: Aggregate, entityName: string): Entity | null {
        return aggregate.entities?.find((e: any) => e.name === entityName) || null;
    }

    

    private buildAggregateIdFieldName(entityName: string): string {
        
        
        const referencedName = this.extractReferencedAggregateName(entityName);
        return `${referencedName.toLowerCase()}AggregateId`;
    }

    

    private extractReferencedAggregateName(entityName: string): string {
        
        

        
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

    

    private determineBusinessKey(entity: Entity | null): string {
        if (!entity || !entity.properties) {
            return 'key';
        }

        
        const commonKeys = ['key', 'code', 'id', 'sequence'];

        for (const keyName of commonKeys) {
            const field = entity.properties.find(p => p.name === keyName);
            if (field) {
                return keyName;
            }
        }

        
        const firstIntField = entity.properties.find(p => {
            const javaType = TypeResolver.resolveJavaType(p.type);
            return javaType === 'Integer' &&
                !p.name.endsWith('AggregateId') &&
                !p.name.endsWith('Version');
        });

        return firstIntField?.name || 'key';
    }

    

    private singularize(word: string): string {
        if (word.endsWith('s')) {
            return word.slice(0, -1);
        }
        return word;
    }

    

    private generateAddMethod(info: CollectionInfo, aggregate: Aggregate, rootEntity: Entity): CollectionMethod {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        const parameters: MethodParameter[] = [
            { type: 'Integer', name: `${lowerEntity}Id` },
            { type: 'Integer', name: info.identifierField },
            { type: `${info.elementType}Dto`, name: `${info.singularName}Dto` },
            { type: 'UnitOfWork', name: 'unitOfWork' }
        ];

        return {
            name: `add${info.capitalizedSingular}`,
            parameters,
            returnType: `${info.elementType}Dto`,
            annotations: [],
            collectionInfo: info,
            operation: 'add',
            aggregateName,
            entityName,
            lowerEntityName: lowerEntity
        } as any;
    }

    

    private generateAddBatchMethod(info: CollectionInfo, aggregate: Aggregate, rootEntity: Entity): CollectionMethod {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        const parameters: MethodParameter[] = [
            { type: 'Integer', name: `${lowerEntity}Id` },
            { type: `List<${info.elementType}Dto>`, name: `${info.singularName}Dtos` },
            { type: 'UnitOfWork', name: 'unitOfWork' }
        ];

        return {
            name: `add${info.capitalizedSingular}s`,
            parameters,
            returnType: `List<${info.elementType}Dto>`,
            annotations: [],
            collectionInfo: info,
            operation: 'addBatch',
            aggregateName,
            entityName,
            lowerEntityName: lowerEntity
        } as any;
    }

    

    private generateGetMethod(info: CollectionInfo, aggregate: Aggregate, rootEntity: Entity): CollectionMethod {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        const parameters: MethodParameter[] = [
            { type: 'Integer', name: `${lowerEntity}Id` },
            { type: info.identifierType, name: info.identifierField },
            { type: 'UnitOfWork', name: 'unitOfWork' }
        ];

        return {
            name: `get${info.capitalizedSingular}`,
            parameters,
            returnType: `${info.elementType}Dto`,
            annotations: [],
            collectionInfo: info,
            operation: 'get',
            aggregateName,
            entityName,
            lowerEntityName: lowerEntity
        } as any;
    }

    

    private generateRemoveMethod(info: CollectionInfo, aggregate: Aggregate, rootEntity: Entity): CollectionMethod {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        const parameters: MethodParameter[] = [
            { type: 'Integer', name: `${lowerEntity}Id` },
            { type: info.identifierType, name: info.identifierField },
            { type: 'UnitOfWork', name: 'unitOfWork' }
        ];

        return {
            name: `remove${info.capitalizedSingular}`,
            parameters,
            returnType: 'void',
            annotations: [],
            collectionInfo: info,
            operation: 'remove',
            aggregateName,
            entityName,
            lowerEntityName: lowerEntity
        } as any;
    }

    

    private generateUpdateMethod(info: CollectionInfo, aggregate: Aggregate, rootEntity: Entity): CollectionMethod {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();

        const parameters: MethodParameter[] = [
            { type: 'Integer', name: `${lowerEntity}Id` },
            { type: info.identifierType, name: info.identifierField },
            { type: `${info.elementType}Dto`, name: `${info.singularName}Dto` },
            { type: 'UnitOfWork', name: 'unitOfWork' }
        ];

        
        const elementEntity = this.findEntityByName(aggregate, info.elementType);
        const updatableFields = this.extractUpdatableFields(elementEntity, info.isProjection);

        return {
            name: `update${info.capitalizedSingular}`,
            parameters,
            returnType: `${info.elementType}Dto`,
            annotations: [],
            collectionInfo: info,
            operation: 'update',
            aggregateName,
            entityName,
            lowerEntityName: lowerEntity,
            updatableFields
        } as any;
    }

    

    private extractUpdatableFields(entity: Entity | null, isProjection: boolean): Array<{ name: string; capitalizedName: string; type: string }> {
        if (!entity || !entity.properties) {
            return [];
        }

        const fields: Array<{ name: string; capitalizedName: string; type: string }> = [];
        const excludedFields = ['aggregateId', 'version', 'state'];

        for (const prop of entity.properties) {
            const propName = prop.name;

            
            if (excludedFields.includes(propName)) continue;

            
            if ((prop as any).isFinal) continue;

            
            if (isProjection && (propName.endsWith('AggregateId') || propName.endsWith('Version') || propName.endsWith('State'))) {
                continue;
            }

            
            const javaType = TypeResolver.resolveJavaType(prop.type);
            if (javaType.startsWith('Set<') || javaType.startsWith('List<')) {
                continue;
            }

            fields.push({
                name: propName,
                capitalizedName: capitalize(propName),
                type: javaType
            });
        }

        return fields;
    }
}
