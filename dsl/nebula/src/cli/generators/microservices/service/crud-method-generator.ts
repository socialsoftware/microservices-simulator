/**
 * CRUD Method Generation System
 * 
 * This module provides reusable CRUD method templates for service generation,
 * eliminating duplicate CRUD generation logic across service generators.
 */

import { Aggregate, Entity } from "../../../../language/generated/ast.js";

/**
 * Generated method structure
 */
export interface GeneratedMethod {
    name: string;
    parameters: MethodParameter[];
    returnType: string;
    annotations: string[];
    implementation?: any[];
}

/**
 * Method parameter structure
 */
export interface MethodParameter {
    type: string;
    name: string;
}

/**
 * CRUD generation options
 */
export interface CrudGenerationOptions {
    includeCreate: boolean;
    includeRead: boolean;
    includeUpdate: boolean;
    includeDelete: boolean;
    includeFindAll: boolean;
    includeCustomQueries: boolean;
    transactional: boolean;
    includeValidation: boolean;
}

/**
 * CRUD method generator with reusable templates
 */
export class CrudMethodGenerator {

    /**
     * Generate all CRUD methods for an aggregate
     */
    generateCrudMethods(
        aggregate: Aggregate,
        rootEntity: Entity,
        options: CrudGenerationOptions = this.getDefaultOptions()
    ): GeneratedMethod[] {
        const methods: GeneratedMethod[] = [];
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        // Extract simple properties for field mapping (exclude complex types)
        const properties = this.extractSimpleProperties(rootEntity);

        if (options.includeCreate) {
            methods.push(this.generateCreateMethod(entityName, lowerEntity, options, properties));
        }

        if (options.includeRead) {
            methods.push(this.generateFindByIdMethod(entityName, options));
        }

        if (options.includeUpdate) {
            methods.push(this.generateUpdateMethod(entityName, lowerEntity, options, properties));
        }

        if (options.includeDelete) {
            methods.push(this.generateDeleteMethod(entityName, options));
        }

        if (options.includeFindAll) {
            methods.push(this.generateFindAllMethod(aggregateName, entityName, options));
        }

        return methods;
    }

    /**
     * Extract simple properties from entity for field mapping
     */
    private extractSimpleProperties(entity: Entity): { name: string; capitalizedName: string }[] {
        const simpleTypes = ['String', 'Integer', 'Long', 'Boolean', 'Double', 'Float', 'LocalDateTime', 'LocalDate', 'BigDecimal'];
        const properties: { name: string; capitalizedName: string }[] = [];

        for (const prop of entity.properties || []) {
            const propType = (prop as any).type;
            // PrimitiveType has typeName, EntityType has type.$refText
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            if (simpleTypes.includes(typeName)) {
                const propName = prop.name;
                properties.push({
                    name: propName,
                    capitalizedName: propName.charAt(0).toUpperCase() + propName.slice(1)
                });
            }
        }

        return properties;
    }

    /**
     * Generate create method with UnitOfWork pattern
     */
    private generateCreateMethod(entityName: string, lowerEntity: string, options: CrudGenerationOptions, properties: { name: string; capitalizedName: string }[]): GeneratedMethod {
        return {
            name: `create${entityName}`,
            parameters: [
                { type: `${entityName}Dto`, name: `${lowerEntity}Dto` },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: `${entityName}Dto`,
            annotations: [],
            crudAction: 'create',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`,
            properties
        } as any;
    }

    /**
     * Generate get by ID method with UnitOfWork pattern
     */
    private generateFindByIdMethod(entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        return {
            name: `get${entityName}ById`,
            parameters: [
                { type: 'Integer', name: 'aggregateId' },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: `${entityName}Dto`,
            annotations: [],
            crudAction: 'findById',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`
        } as any;
    }

    /**
     * Generate update method with UnitOfWork pattern
     */
    private generateUpdateMethod(entityName: string, lowerEntity: string, options: CrudGenerationOptions, properties: { name: string; capitalizedName: string }[]): GeneratedMethod {
        return {
            name: `update${entityName}`,
            parameters: [
                { type: 'Integer', name: 'aggregateId' },
                { type: `${entityName}Dto`, name: `${lowerEntity}Dto` },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: `${entityName}Dto`,
            annotations: [],
            crudAction: 'update',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`,
            properties
        } as any;
    }

    /**
     * Generate delete method with UnitOfWork pattern
     */
    private generateDeleteMethod(entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        return {
            name: `delete${entityName}`,
            parameters: [
                { type: 'Integer', name: 'aggregateId' },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: 'void',
            annotations: [],
            crudAction: 'delete',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`
        } as any;
    }

    /**
     * Generate find all method
     */
    private generateFindAllMethod(aggregateName: string, entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const lowerEntity = entityName.charAt(0).toLowerCase() + entityName.slice(1);

        return {
            name: `getAll${aggregateName}s`,
            parameters: [
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ],
            returnType: `List<${entityName}Dto>`,
            annotations: [],
            crudAction: 'findAll',
            entityName,
            lowerEntityName: lowerEntity,
            lowerRepositoryName: `${lowerEntity}Repository`
        } as any;
    }

    /**
     * Get default CRUD generation options
     */
    private getDefaultOptions(): CrudGenerationOptions {
        return {
            includeCreate: true,
            includeRead: true,
            includeUpdate: true,
            includeDelete: true,
            includeFindAll: true,
            includeCustomQueries: false,
            transactional: true,
            includeValidation: true
        };
    }

    /**
     * Create CRUD options with overrides
     */
    static createOptions(overrides: Partial<CrudGenerationOptions> = {}): CrudGenerationOptions {
        const defaults: CrudGenerationOptions = {
            includeCreate: true,
            includeRead: true,
            includeUpdate: true,
            includeDelete: true,
            includeFindAll: true,
            includeCustomQueries: false,
            transactional: true,
            includeValidation: true
        };

        return { ...defaults, ...overrides };
    }
}
