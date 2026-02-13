import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../common/unified-type-resolver.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { GeneratedMethod, MethodParameter } from "./crud-method-generator.js";

export interface CollectionInfo {
    propertyName: string;           // 'users', 'options'
    elementType: string;            // 'ExecutionUser', 'Option'
    isProjection: boolean;          // true if has aggregateRef
    identifierField: string;        // 'userAggregateId' or 'key'
    identifierType: string;         // 'Integer'
    collectionType: 'Set' | 'List';
    singularName: string;           // 'user', 'option'
    capitalizedSingular: string;    // 'User', 'Option'
    capitalizedCollection: string;  // 'Users', 'Options'
}

export interface CollectionMethod extends GeneratedMethod {
    collectionInfo: CollectionInfo;
    operation: 'add' | 'addBatch' | 'get' | 'remove' | 'update';
}

export class CollectionMethodGenerator {

    /**
     * Generate all collection manipulation methods for an aggregate
     */
    generateCollectionMethods(aggregate: Aggregate, rootEntity: Entity): CollectionMethod[] {
        const methods: CollectionMethod[] = [];
        const collections = this.findCollectionProperties(rootEntity);

        for (const collection of collections) {
            const collectionInfo = this.buildCollectionInfo(collection, aggregate);

            // Generate 5 methods per collection
            methods.push(this.generateAddMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateAddBatchMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateGetMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateRemoveMethod(collectionInfo, aggregate, rootEntity));
            methods.push(this.generateUpdateMethod(collectionInfo, aggregate, rootEntity));
        }

        return methods;
    }

    /**
     * Find all collection properties in the root entity
     */
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

    /**
     * Build complete collection information including identifier strategy
     */
    private buildCollectionInfo(
        collection: { name: string; elementType: string; type: 'Set' | 'List' },
        aggregate: Aggregate
    ): CollectionInfo {
        const elementEntity = this.findEntityByName(aggregate, collection.elementType);
        const isProjection = this.hasAggregateRef(elementEntity);

        // Determine identifier field
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

    /**
     * Check if entity has aggregate reference (is projection entity)
     */
    private hasAggregateRef(entity: Entity | null): boolean {
        if (!entity) return false;
        return (entity as any).aggregateRef !== undefined && (entity as any).aggregateRef !== null;
    }

    /**
     * Find entity by name in aggregate
     */
    private findEntityByName(aggregate: Aggregate, entityName: string): Entity | null {
        return aggregate.entities?.find((e: any) => e.name === entityName) || null;
    }

    /**
     * Build aggregateId field name for projection entity
     * ExecutionUser -> userAggregateId
     */
    private buildAggregateIdFieldName(entityName: string): string {
        // Extract the referenced aggregate name from entity name
        // ExecutionUser -> user, AnswerQuestion -> question
        const referencedName = this.extractReferencedAggregateName(entityName);
        return `${referencedName.toLowerCase()}AggregateId`;
    }

    /**
     * Extract referenced aggregate name from projection entity name
     * ExecutionUser -> User, AnswerQuestion -> Question
     */
    private extractReferencedAggregateName(entityName: string): string {
        // Common patterns: ExecutionUser, AnswerQuestion, TournamentParticipant
        // Strategy: Take the part after the first capital letter sequence

        // Find the last capital letter that starts a new word
        for (let i = entityName.length - 1; i >= 0; i--) {
            if (entityName[i] === entityName[i].toUpperCase() && i > 0) {
                const candidate = entityName.substring(i);
                // Check if it's a valid word (not just a single letter)
                if (candidate.length > 1) {
                    return candidate;
                }
            }
        }

        return entityName;
    }

    /**
     * Determine business key field for dto entity
     */
    private determineBusinessKey(entity: Entity | null): string {
        if (!entity || !entity.properties) {
            return 'key';
        }

        // Look for common business key field names
        const commonKeys = ['key', 'code', 'id', 'sequence'];

        for (const keyName of commonKeys) {
            const field = entity.properties.find(p => p.name === keyName);
            if (field) {
                return keyName;
            }
        }

        // Fallback: first Integer field that's not aggregateId/version
        const firstIntField = entity.properties.find(p => {
            const javaType = TypeResolver.resolveJavaType(p.type);
            return javaType === 'Integer' &&
                !p.name.endsWith('AggregateId') &&
                !p.name.endsWith('Version');
        });

        return firstIntField?.name || 'key';
    }

    /**
     * Simple singularization (removes trailing 's')
     */
    private singularize(word: string): string {
        if (word.endsWith('s')) {
            return word.slice(0, -1);
        }
        return word;
    }

    /**
     * Generate add single element method
     */
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

    /**
     * Generate add multiple elements method
     */
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

    /**
     * Generate get single element method
     */
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

    /**
     * Generate remove element method
     */
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

    /**
     * Generate update element method
     */
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

        // Get updatable fields from the element entity
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

    /**
     * Extract updatable fields from entity (non-final, non-id fields)
     */
    private extractUpdatableFields(entity: Entity | null, isProjection: boolean): Array<{ name: string; capitalizedName: string; type: string }> {
        if (!entity || !entity.properties) {
            return [];
        }

        const fields: Array<{ name: string; capitalizedName: string; type: string }> = [];
        const excludedFields = ['aggregateId', 'version', 'state'];

        for (const prop of entity.properties) {
            const propName = prop.name;

            // Skip excluded fields
            if (excludedFields.includes(propName)) continue;

            // Skip final fields
            if ((prop as any).isFinal) continue;

            // Skip aggregateId/version fields in projection entities
            if (isProjection && (propName.endsWith('AggregateId') || propName.endsWith('Version') || propName.endsWith('State'))) {
                continue;
            }

            // Skip collection fields
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
