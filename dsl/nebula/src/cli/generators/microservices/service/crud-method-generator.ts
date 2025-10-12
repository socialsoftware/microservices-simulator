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
        const lowerEntity = entityName.toLowerCase();

        if (options.includeCreate) {
            methods.push(this.generateCreateMethod(entityName, lowerEntity, options));
        }

        if (options.includeRead) {
            methods.push(this.generateFindByIdMethod(entityName, options));
        }

        if (options.includeUpdate) {
            methods.push(this.generateUpdateMethod(entityName, lowerEntity, options));
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
     * Generate create method
     */
    private generateCreateMethod(entityName: string, lowerEntity: string, options: CrudGenerationOptions): GeneratedMethod {
        const annotations = [];
        if (options.transactional) {
            annotations.push('@Transactional');
        }

        return {
            name: `create${entityName}`,
            parameters: [{ type: `${entityName}Dto`, name: `${lowerEntity}Dto` }],
            returnType: entityName,
            annotations,
            implementation: this.generateCreateImplementation(entityName, lowerEntity)
        };
    }

    /**
     * Generate find by ID method
     */
    private generateFindByIdMethod(entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const annotations = [];
        if (options.transactional) {
            annotations.push('@Transactional(readOnly = true)');
        }

        return {
            name: `find${entityName}ById`,
            parameters: [{ type: 'Integer', name: 'id' }],
            returnType: `Optional<${entityName}>`,
            annotations,
            implementation: this.generateFindByIdImplementation(entityName)
        };
    }

    /**
     * Generate update method
     */
    private generateUpdateMethod(entityName: string, lowerEntity: string, options: CrudGenerationOptions): GeneratedMethod {
        const annotations = [];
        if (options.transactional) {
            annotations.push('@Transactional');
        }

        return {
            name: `update${entityName}`,
            parameters: [
                { type: 'Integer', name: 'id' },
                { type: `${entityName}Dto`, name: `${lowerEntity}Dto` }
            ],
            returnType: entityName,
            annotations,
            implementation: this.generateUpdateImplementation(entityName, lowerEntity)
        };
    }

    /**
     * Generate delete method
     */
    private generateDeleteMethod(entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const annotations = [];
        if (options.transactional) {
            annotations.push('@Transactional');
        }

        return {
            name: `delete${entityName}`,
            parameters: [{ type: 'Integer', name: 'id' }],
            returnType: 'void',
            annotations,
            implementation: this.generateDeleteImplementation(entityName)
        };
    }

    /**
     * Generate find all method
     */
    private generateFindAllMethod(aggregateName: string, entityName: string, options: CrudGenerationOptions): GeneratedMethod {
        const annotations = [];
        if (options.transactional) {
            annotations.push('@Transactional(readOnly = true)');
        }

        return {
            name: `findAll${aggregateName}s`,
            parameters: [],
            returnType: `List<${entityName}>`,
            annotations,
            implementation: this.generateFindAllImplementation(aggregateName)
        };
    }

    /**
     * Implementation generators (these would be used by templates)
     */
    private generateCreateImplementation(entityName: string, lowerEntity: string): any[] {
        return [
            {
                action: 'validate',
                condition: `${lowerEntity}Dto != null`,
                exception: 'IllegalArgumentException',
                exceptionParams: [`"${entityName}Dto cannot be null"`]
            },
            {
                action: 'create',
                entityVar: lowerEntity,
                entityType: entityName,
                constructorParam: `${lowerEntity}Dto`
            },
            {
                action: 'save',
                entityVar: lowerEntity
            },
            {
                action: 'return',
                returnValue: lowerEntity
            }
        ];
    }

    private generateFindByIdImplementation(entityName: string): any[] {
        return [
            {
                action: 'validate',
                condition: 'id != null && id > 0',
                exception: 'IllegalArgumentException',
                exceptionParams: ['"ID must be positive"']
            },
            {
                action: 'findById',
                entityType: entityName,
                idVar: 'id'
            }
        ];
    }

    private generateUpdateImplementation(entityName: string, lowerEntity: string): any[] {
        return [
            {
                action: 'validate',
                condition: `id != null && id > 0 && ${lowerEntity}Dto != null`,
                exception: 'IllegalArgumentException',
                exceptionParams: ['"Invalid parameters for update"']
            },
            {
                action: 'load',
                entityVar: 'existing' + entityName,
                entityType: entityName,
                aggregateId: 'id'
            },
            {
                action: 'update',
                entityVar: 'existing' + entityName,
                updateData: `${lowerEntity}Dto`
            },
            {
                action: 'save',
                entityVar: 'existing' + entityName
            },
            {
                action: 'return',
                returnValue: 'existing' + entityName
            }
        ];
    }

    private generateDeleteImplementation(entityName: string): any[] {
        return [
            {
                action: 'validate',
                condition: 'id != null && id > 0',
                exception: 'IllegalArgumentException',
                exceptionParams: ['"ID must be positive"']
            },
            {
                action: 'load',
                entityVar: 'existing' + entityName,
                entityType: entityName,
                aggregateId: 'id'
            },
            {
                action: 'delete',
                entityVar: 'existing' + entityName
            }
        ];
    }

    private generateFindAllImplementation(aggregateName: string): any[] {
        return [
            {
                action: 'findAll',
                entityType: aggregateName,
                returnType: `List<${aggregateName}>`
            }
        ];
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
