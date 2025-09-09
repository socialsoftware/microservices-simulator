import { Model, Aggregate, Entity, Property } from "../../../language/generated/ast.js";

export interface RelationshipMap {
    oneToMany: OneToManyRelationship[];
    manyToOne: ManyToOneRelationship[];
    manyToMany: ManyToManyRelationship[];
    allRelationships: Relationship[];
}

export interface Relationship {
    id: string;
    type: 'OneToMany' | 'ManyToOne' | 'ManyToMany';
    sourceEntity: string;
    targetEntity: string;
    sourceProperty?: string;
    targetProperty?: string;
    isBidirectional: boolean;
    cascadeType?: string;
    fetchType?: string;
}

export interface OneToManyRelationship extends Relationship {
    type: 'OneToMany';
    sourceProperty: string;
    targetProperty: string;
    mappedBy?: string;
}

export interface ManyToOneRelationship extends Relationship {
    type: 'ManyToOne';
    sourceProperty: string;
    targetEntity: string;
    joinColumn?: string;
}

export interface ManyToManyRelationship extends Relationship {
    type: 'ManyToMany';
    sourceProperty: string;
    targetProperty: string;
    joinTable?: string;
    sourceJoinColumn?: string;
    targetJoinColumn?: string;
}

export class RelationshipAnalyzer {
    private relationships: RelationshipMap;

    constructor() {
        this.relationships = {
            oneToMany: [],
            manyToOne: [],
            manyToMany: [],
            allRelationships: []
        };
    }

    analyze(model: Model): RelationshipMap {
        this.reset();

        if (!model.aggregates) {
            return this.relationships;
        }

        for (const aggregate of model.aggregates) {
            this.analyzeAggregate(aggregate);
        }

        return this.relationships;
    }

    private analyzeAggregate(aggregate: Aggregate): void {
        if (!aggregate.entities) {
            return;
        }

        for (const entity of aggregate.entities) {
            this.analyzeEntityRelationships(entity, aggregate.entities);
        }
    }

    private analyzeEntityRelationships(entity: Entity, allEntities: Entity[]): void {
        if (!entity.properties) {
            return;
        }

        for (const property of entity.properties) {
            this.analyzePropertyRelationship(entity, property, allEntities);
        }
    }

    private analyzePropertyRelationship(entity: Entity, property: Property, allEntities: Entity[]): void {
        const propertyType = this.getPropertyType(property);
        const targetEntity = this.findEntityByType(propertyType, allEntities);

        if (!targetEntity) {
            return;
        }

        const relationship = this.determineRelationshipType(entity, property, targetEntity);
        if (relationship) {
            this.addRelationship(relationship);
        }
    }

    private determineRelationshipType(sourceEntity: Entity, property: Property, targetEntity: Entity): Relationship | null {
        const propertyType = this.getPropertyType(property);
        const isCollection = this.isCollectionType(propertyType);
        const isOptional = this.isOptionalType(propertyType);

        if (isCollection) {
            return this.createOneToManyRelationship(sourceEntity, property, targetEntity);
        }

        if (!isCollection && !isOptional) {
            return this.createManyToOneRelationship(sourceEntity, property, targetEntity);
        }

        if (this.isManyToManyCandidate(sourceEntity, property, targetEntity)) {
            return this.createManyToManyRelationship(sourceEntity, property, targetEntity);
        }

        return null;
    }

    private createOneToManyRelationship(sourceEntity: Entity, property: Property, targetEntity: Entity): OneToManyRelationship {
        const relationshipId = `${sourceEntity.name}_${property.name}_${targetEntity.name}`;

        return {
            id: relationshipId,
            type: 'OneToMany',
            sourceEntity: sourceEntity.name,
            targetEntity: targetEntity.name,
            sourceProperty: property.name,
            targetProperty: this.generateTargetPropertyName(sourceEntity.name),
            isBidirectional: false,
            cascadeType: 'ALL',
            fetchType: 'LAZY',
            mappedBy: this.generateMappedByPropertyName(sourceEntity.name)
        };
    }

    private createManyToOneRelationship(sourceEntity: Entity, property: Property, targetEntity: Entity): ManyToOneRelationship {
        const relationshipId = `${sourceEntity.name}_${property.name}_${targetEntity.name}`;

        return {
            id: relationshipId,
            type: 'ManyToOne',
            sourceEntity: sourceEntity.name,
            targetEntity: targetEntity.name,
            sourceProperty: property.name,
            isBidirectional: false,
            cascadeType: 'PERSIST',
            fetchType: 'EAGER',
            joinColumn: this.generateJoinColumnName(property.name)
        };
    }

    private createManyToManyRelationship(sourceEntity: Entity, property: Property, targetEntity: Entity): ManyToManyRelationship {
        const relationshipId = `${sourceEntity.name}_${property.name}_${targetEntity.name}`;

        return {
            id: relationshipId,
            type: 'ManyToMany',
            sourceEntity: sourceEntity.name,
            targetEntity: targetEntity.name,
            sourceProperty: property.name,
            targetProperty: this.generateTargetPropertyName(sourceEntity.name),
            isBidirectional: false,
            cascadeType: 'PERSIST',
            fetchType: 'LAZY',
            joinTable: this.generateJoinTableName(sourceEntity.name, targetEntity.name),
            sourceJoinColumn: this.generateSourceJoinColumnName(sourceEntity.name),
            targetJoinColumn: this.generateTargetJoinColumnName(targetEntity.name)
        };
    }

    private addRelationship(relationship: Relationship): void {
        this.relationships.allRelationships.push(relationship);

        switch (relationship.type) {
            case 'OneToMany':
                this.relationships.oneToMany.push(relationship as OneToManyRelationship);
                break;
            case 'ManyToOne':
                this.relationships.manyToOne.push(relationship as ManyToOneRelationship);
                break;
            case 'ManyToMany':
                this.relationships.manyToMany.push(relationship as ManyToManyRelationship);
                break;
        }
    }

    private getPropertyType(property: Property): string {
        if (property.type) {
            if (typeof property.type === 'string') {
                return property.type;
            } else if (property.type.$type === 'EntityType') {
                return (property.type as any).name || 'String';
            } else if (property.type.$type === 'PrimitiveType') {
                return (property.type as any).name || 'String';
            }
        }
        return 'String';
    }

    private isCollectionType(type: string): boolean {
        return type.includes('List') || type.includes('Set') || type.includes('Collection') || type.includes('[]');
    }

    private isOptionalType(type: string): boolean {
        return type.includes('Optional') || type.includes('?');
    }

    private findEntityByType(typeName: string, entities: Entity[]): Entity | null {
        // Clean the type name (remove generics, arrays, etc.)
        const cleanTypeName = this.cleanTypeName(typeName);

        return entities.find(entity => entity.name === cleanTypeName) || null;
    }

    private cleanTypeName(typeName: string): string {
        return typeName
            .replace(/<.*>/, '') // Remove generics
            .replace(/\[\]$/, '') // Remove array brackets
            .replace(/Optional<|>/, '') // Remove Optional wrapper
            .trim();
    }

    private isManyToManyCandidate(sourceEntity: Entity, property: Property, targetEntity: Entity): boolean {
        return this.isCollectionType(this.getPropertyType(property)) &&
            this.hasBidirectionalRelationship(sourceEntity, targetEntity);
    }

    private hasBidirectionalRelationship(sourceEntity: Entity, targetEntity: Entity): boolean {
        return false;
    }

    private generateTargetPropertyName(sourceEntityName: string): string {
        return `${sourceEntityName.toLowerCase()}s`;
    }

    private generateMappedByPropertyName(sourceEntityName: string): string {
        return `${sourceEntityName.toLowerCase()}`;
    }

    private generateJoinColumnName(propertyName: string): string {
        return `${propertyName}_id`;
    }

    private generateJoinTableName(sourceEntityName: string, targetEntityName: string): string {
        const sortedNames = [sourceEntityName, targetEntityName].sort();
        return `${sortedNames[0]}_${sortedNames[1]}`;
    }

    private generateSourceJoinColumnName(sourceEntityName: string): string {
        return `${sourceEntityName.toLowerCase()}_id`;
    }


    private generateTargetJoinColumnName(targetEntityName: string): string {
        return `${targetEntityName.toLowerCase()}_id`;
    }


    private reset(): void {
        this.relationships = {
            oneToMany: [],
            manyToOne: [],
            manyToMany: [],
            allRelationships: []
        };
    }

    getStatistics(): { total: number; oneToMany: number; manyToOne: number; manyToMany: number } {
        return {
            total: this.relationships.allRelationships.length,
            oneToMany: this.relationships.oneToMany.length,
            manyToOne: this.relationships.manyToOne.length,
            manyToMany: this.relationships.manyToMany.length
        };
    }

    getRelationshipsForEntity(entityName: string): Relationship[] {
        return this.relationships.allRelationships.filter(
            rel => rel.sourceEntity === entityName || rel.targetEntity === entityName
        );
    }

    hasRelationship(entity1: string, entity2: string): boolean {
        return this.relationships.allRelationships.some(
            rel => (rel.sourceEntity === entity1 && rel.targetEntity === entity2) ||
                (rel.sourceEntity === entity2 && rel.targetEntity === entity1)
        );
    }
}
