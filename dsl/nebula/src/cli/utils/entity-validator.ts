/**
 * Entity Validation Utilities
 * 
 * This module provides comprehensive validation utilities for aggregates, entities,
 * and service definitions, centralizing all validation logic that was previously
 * scattered across multiple generators.
 */

import { Aggregate, Entity } from "../../language/generated/ast.js";

/**
 * Validation result with detailed information
 */
export interface ValidationResult {
    isValid: boolean;
    errors: ValidationError[];
    warnings: ValidationWarning[];
    entity?: string;
    aggregate?: string;
}

/**
 * Validation error with context
 */
export interface ValidationError {
    code: string;
    message: string;
    field?: string;
    value?: any;
    suggestion?: string;
}

/**
 * Validation warning with context
 */
export interface ValidationWarning {
    code: string;
    message: string;
    field?: string;
    value?: any;
    suggestion?: string;
}

/**
 * Validation options
 */
export interface ValidationOptions {
    strictMode: boolean;
    checkNamingConventions: boolean;
    validateRelationships: boolean;
    requireDocumentation: boolean;
    customRules?: ValidationRule[];
}

/**
 * Custom validation rule
 */
export interface ValidationRule {
    name: string;
    validate: (entity: Entity | Aggregate) => ValidationError[];
}

/**
 * Comprehensive entity validator with centralized validation logic
 */
export class EntityValidator {
    private options: ValidationOptions;

    constructor(options?: ValidationOptions) {
        this.options = options || this.getDefaultOptions();
    }

    /**
     * Validate an aggregate with all its entities
     */
    validateAggregate(aggregate: Aggregate): ValidationResult {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        try {
            // Basic aggregate validation
            errors.push(...this.validateAggregateStructure(aggregate));

            // Entity validation
            errors.push(...this.validateAggregateEntities(aggregate));

            // Relationship validation
            if (this.options.validateRelationships) {
                errors.push(...this.validateEntityRelationships(aggregate));
            }

            // Naming convention validation
            if (this.options.checkNamingConventions) {
                warnings.push(...this.validateAggregateNaming(aggregate));
            }

            // Custom rule validation
            if (this.options.customRules) {
                for (const rule of this.options.customRules) {
                    errors.push(...rule.validate(aggregate));
                }
            }

        } catch (error) {
            errors.push({
                code: 'VALIDATION_ERROR',
                message: `Validation failed: ${error instanceof Error ? error.message : String(error)}`
            });
        }

        return {
            isValid: errors.length === 0,
            errors,
            warnings,
            aggregate: aggregate.name
        };
    }

    /**
     * Validate a single entity
     */
    validateEntity(entity: Entity, aggregate?: Aggregate): ValidationResult {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        try {
            // Basic entity validation
            errors.push(...this.validateEntityStructure(entity));

            // Property validation
            errors.push(...this.validateEntityProperties(entity));

            // Naming convention validation
            if (this.options.checkNamingConventions) {
                warnings.push(...this.validateEntityNaming(entity));
            }

            // Context validation (if aggregate provided)
            if (aggregate) {
                errors.push(...this.validateEntityInContext(entity, aggregate));
            }

        } catch (error) {
            errors.push({
                code: 'ENTITY_VALIDATION_ERROR',
                message: `Entity validation failed: ${error instanceof Error ? error.message : String(error)}`
            });
        }

        return {
            isValid: errors.length === 0,
            errors,
            warnings,
            entity: entity.name,
            aggregate: aggregate?.name
        };
    }

    /**
     * Validate a service definition
     */
    validateServiceDefinition(serviceDefinition: any, aggregate: Aggregate): ValidationResult {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        try {
            // Basic service validation
            errors.push(...this.validateServiceStructure(serviceDefinition, aggregate));

            // Method validation
            if (serviceDefinition.serviceMethods) {
                errors.push(...this.validateServiceMethods(serviceDefinition.serviceMethods, aggregate));
            }

            // CRUD validation
            if (aggregate.generateCrud) {
                warnings.push(...this.validateCrudConfiguration(serviceDefinition, aggregate));
            }

        } catch (error) {
            errors.push({
                code: 'SERVICE_VALIDATION_ERROR',
                message: `Service validation failed: ${error instanceof Error ? error.message : String(error)}`
            });
        }

        return {
            isValid: errors.length === 0,
            errors,
            warnings,
            aggregate: aggregate.name
        };
    }

    /**
     * Aggregate structure validation
     */
    private validateAggregateStructure(aggregate: Aggregate): ValidationError[] {
        const errors: ValidationError[] = [];

        if (!aggregate.name) {
            errors.push({
                code: 'MISSING_AGGREGATE_NAME',
                message: 'Aggregate must have a name'
            });
        }

        if (!aggregate.entities || aggregate.entities.length === 0) {
            errors.push({
                code: 'NO_ENTITIES',
                message: 'Aggregate must have at least one entity',
                suggestion: 'Add at least one entity to the aggregate'
            });
        }

        return errors;
    }

    /**
     * Aggregate entities validation
     */
    private validateAggregateEntities(aggregate: Aggregate): ValidationError[] {
        const errors: ValidationError[] = [];

        if (!aggregate.entities) return errors;

        const rootEntities = aggregate.entities.filter((e: any) => e.isRoot);

        if (rootEntities.length === 0) {
            errors.push({
                code: 'NO_ROOT_ENTITY',
                message: 'Aggregate must have exactly one root entity',
                suggestion: 'Mark one entity as root using "isRoot: true"'
            });
        } else if (rootEntities.length > 1) {
            errors.push({
                code: 'MULTIPLE_ROOT_ENTITIES',
                message: 'Aggregate can have only one root entity',
                value: rootEntities.map(e => e.name),
                suggestion: 'Mark only one entity as root'
            });
        }

        // Validate each entity
        for (const entity of aggregate.entities) {
            const entityValidation = this.validateEntity(entity, aggregate);
            errors.push(...entityValidation.errors);
        }

        return errors;
    }

    /**
     * Entity structure validation
     */
    private validateEntityStructure(entity: Entity): ValidationError[] {
        const errors: ValidationError[] = [];

        if (!entity.name) {
            errors.push({
                code: 'MISSING_ENTITY_NAME',
                message: 'Entity must have a name'
            });
        }

        if (!entity.properties || entity.properties.length === 0) {
            errors.push({
                code: 'NO_PROPERTIES',
                message: 'Entity should have at least one property',
                suggestion: 'Add properties to define the entity structure'
            });
        }

        return errors;
    }

    /**
     * Entity properties validation
     */
    private validateEntityProperties(entity: Entity): ValidationError[] {
        const errors: ValidationError[] = [];

        if (!entity.properties) return errors;

        const propertyNames = new Set<string>();

        for (const property of entity.properties) {
            // Check for duplicate property names
            if (propertyNames.has(property.name)) {
                errors.push({
                    code: 'DUPLICATE_PROPERTY',
                    message: `Duplicate property name: ${property.name}`,
                    field: property.name,
                    suggestion: 'Use unique property names within an entity'
                });
            }
            propertyNames.add(property.name);

            // Validate property structure
            if (!property.name) {
                errors.push({
                    code: 'MISSING_PROPERTY_NAME',
                    message: 'Property must have a name'
                });
            }

            if (!property.type) {
                errors.push({
                    code: 'MISSING_PROPERTY_TYPE',
                    message: `Property ${property.name} must have a type`,
                    field: property.name
                });
            }
        }

        return errors;
    }

    /**
     * Entity relationship validation
     */
    private validateEntityRelationships(aggregate: Aggregate): ValidationError[] {
        const errors: ValidationError[] = [];

        // This would implement complex relationship validation
        // For now, just basic checks

        return errors;
    }

    /**
     * Naming convention validation
     */
    private validateAggregateNaming(aggregate: Aggregate): ValidationWarning[] {
        const warnings: ValidationWarning[] = [];

        if (aggregate.name && !this.isPascalCase(aggregate.name)) {
            warnings.push({
                code: 'NAMING_CONVENTION',
                message: `Aggregate name should be PascalCase: ${aggregate.name}`,
                field: 'name',
                value: aggregate.name,
                suggestion: `Consider renaming to ${this.toPascalCase(aggregate.name)}`
            });
        }

        return warnings;
    }

    private validateEntityNaming(entity: Entity): ValidationWarning[] {
        const warnings: ValidationWarning[] = [];

        if (entity.name && !this.isPascalCase(entity.name)) {
            warnings.push({
                code: 'NAMING_CONVENTION',
                message: `Entity name should be PascalCase: ${entity.name}`,
                field: 'name',
                value: entity.name,
                suggestion: `Consider renaming to ${this.toPascalCase(entity.name)}`
            });
        }

        return warnings;
    }

    /**
     * Entity context validation
     */
    private validateEntityInContext(entity: Entity, aggregate: Aggregate): ValidationError[] {
        const errors: ValidationError[] = [];

        // Validate that entity belongs to the aggregate
        const entityInAggregate = aggregate.entities.some(e => e.name === entity.name);
        if (!entityInAggregate) {
            errors.push({
                code: 'ENTITY_NOT_IN_AGGREGATE',
                message: `Entity ${entity.name} is not part of aggregate ${aggregate.name}`
            });
        }

        return errors;
    }

    /**
     * Service definition validation
     */
    private validateServiceStructure(serviceDefinition: any, aggregate: Aggregate): ValidationError[] {
        const errors: ValidationError[] = [];

        if (!serviceDefinition.name && !aggregate.generateCrud) {
            errors.push({
                code: 'MISSING_SERVICE_NAME',
                message: 'Service definition must have a name or enable CRUD generation',
                suggestion: 'Add a service name or use @GenerateCrud at aggregate level'
            });
        }

        return errors;
    }

    private validateServiceMethods(serviceMethods: any[], aggregate: Aggregate): ValidationError[] {
        const errors: ValidationError[] = [];

        const methodNames = new Set<string>();

        for (const method of serviceMethods) {
            // Check for duplicate method names
            if (methodNames.has(method.name)) {
                errors.push({
                    code: 'DUPLICATE_METHOD',
                    message: `Duplicate service method name: ${method.name}`,
                    field: method.name
                });
            }
            methodNames.add(method.name);

            // Validate method structure
            if (!method.name) {
                errors.push({
                    code: 'MISSING_METHOD_NAME',
                    message: 'Service method must have a name'
                });
            }
        }

        return errors;
    }

    private validateCrudConfiguration(serviceDefinition: any, aggregate: Aggregate): ValidationWarning[] {
        const warnings: ValidationWarning[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            warnings.push({
                code: 'CRUD_WITHOUT_ROOT_ENTITY',
                message: 'CRUD generation enabled but no root entity found',
                suggestion: 'Ensure aggregate has a root entity for CRUD operations'
            });
        }

        return warnings;
    }

    /**
     * Utility methods
     */
    private isPascalCase(str: string): boolean {
        return /^[A-Z][a-zA-Z0-9]*$/.test(str);
    }

    private toPascalCase(str: string): string {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    private getDefaultOptions(): ValidationOptions {
        return {
            strictMode: false,
            checkNamingConventions: true,
            validateRelationships: true,
            requireDocumentation: false,
            customRules: []
        };
    }

    /**
     * Static convenience methods
     */
    static validateAggregate(aggregate: Aggregate, options?: ValidationOptions): ValidationResult {
        const validator = new EntityValidator(options);
        return validator.validateAggregate(aggregate);
    }

    static validateEntity(entity: Entity, aggregate?: Aggregate, options?: ValidationOptions): ValidationResult {
        const validator = new EntityValidator(options);
        return validator.validateEntity(entity, aggregate);
    }

    static validateServiceDefinition(serviceDefinition: any, aggregate: Aggregate, options?: ValidationOptions): ValidationResult {
        const validator = new EntityValidator(options);
        return validator.validateServiceDefinition(serviceDefinition, aggregate);
    }

    /**
     * Batch validation methods
     */
    static validateMultipleAggregates(aggregates: Aggregate[], options?: ValidationOptions): ValidationResult[] {
        const validator = new EntityValidator(options);
        return aggregates.map(aggregate => validator.validateAggregate(aggregate));
    }

    static validateAllEntitiesInAggregate(aggregate: Aggregate, options?: ValidationOptions): ValidationResult[] {
        const validator = new EntityValidator(options);
        return aggregate.entities.map(entity => validator.validateEntity(entity, aggregate));
    }

    /**
     * Validation summary methods
     */
    static summarizeValidationResults(results: ValidationResult[]): {
        totalValidated: number;
        validCount: number;
        errorCount: number;
        warningCount: number;
        mostCommonErrors: string[];
        mostCommonWarnings: string[];
    } {
        const totalValidated = results.length;
        const validCount = results.filter(r => r.isValid).length;
        const allErrors = results.flatMap(r => r.errors);
        const allWarnings = results.flatMap(r => r.warnings);

        // Count error frequencies
        const errorCounts = new Map<string, number>();
        allErrors.forEach(error => {
            const count = errorCounts.get(error.code) || 0;
            errorCounts.set(error.code, count + 1);
        });

        // Count warning frequencies
        const warningCounts = new Map<string, number>();
        allWarnings.forEach(warning => {
            const count = warningCounts.get(warning.code) || 0;
            warningCounts.set(warning.code, count + 1);
        });

        // Get most common issues
        const mostCommonErrors = Array.from(errorCounts.entries())
            .sort((a, b) => b[1] - a[1])
            .slice(0, 5)
            .map(([code]) => code);

        const mostCommonWarnings = Array.from(warningCounts.entries())
            .sort((a, b) => b[1] - a[1])
            .slice(0, 5)
            .map(([code]) => code);

        return {
            totalValidated,
            validCount,
            errorCount: allErrors.length,
            warningCount: allWarnings.length,
            mostCommonErrors,
            mostCommonWarnings
        };
    }

    /**
     * Validation reporting
     */
    static printValidationReport(results: ValidationResult[]): void {
        const summary = this.summarizeValidationResults(results);

        console.log('\n📋 Validation Report:');
        console.log(`   📊 Total validated: ${summary.totalValidated}`);
        console.log(`   ✅ Valid: ${summary.validCount}`);
        console.log(`   ❌ Errors: ${summary.errorCount}`);
        console.log(`   ⚠️  Warnings: ${summary.warningCount}`);

        if (summary.mostCommonErrors.length > 0) {
            console.log(`   🔴 Most common errors: ${summary.mostCommonErrors.join(', ')}`);
        }

        if (summary.mostCommonWarnings.length > 0) {
            console.log(`   🟡 Most common warnings: ${summary.mostCommonWarnings.join(', ')}`);
        }

        // Print detailed errors for invalid results
        const invalidResults = results.filter(r => !r.isValid);
        if (invalidResults.length > 0 && invalidResults.length <= 10) {
            console.log('\n❌ Validation Errors:');
            invalidResults.forEach(result => {
                const entityInfo = result.entity ? `Entity ${result.entity}` : `Aggregate ${result.aggregate}`;
                console.log(`   ${entityInfo}:`);
                result.errors.forEach(error => {
                    console.log(`     • ${error.message} (${error.code})`);
                });
            });
        }
    }
}

/**
 * Validation rule library with common validation rules
 */
export class ValidationRuleLibrary {
    /**
     * Rule: Entity must have an ID property
     */
    static requireIdProperty(): ValidationRule {
        return {
            name: 'require-id-property',
            validate: (entity: Entity | Aggregate) => {
                if ('properties' in entity && entity.properties) {
                    const hasId = entity.properties.some(p => p.name.toLowerCase() === 'id');
                    if (!hasId) {
                        return [{
                            code: 'MISSING_ID_PROPERTY',
                            message: `Entity ${entity.name} should have an ID property`,
                            suggestion: 'Add an "id" property of type Integer'
                        }];
                    }
                }
                return [];
            }
        };
    }

    /**
     * Rule: Aggregate name should match root entity name
     */
    static aggregateNameMatchesRootEntity(): ValidationRule {
        return {
            name: 'aggregate-name-matches-root',
            validate: (aggregate: Entity | Aggregate) => {
                if ('entities' in aggregate && aggregate.entities) {
                    const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
                    if (rootEntity && rootEntity.name !== aggregate.name) {
                        return [{
                            code: 'AGGREGATE_ROOT_NAME_MISMATCH',
                            message: `Aggregate name "${aggregate.name}" should match root entity name "${rootEntity.name}"`,
                            suggestion: `Consider renaming aggregate to "${rootEntity.name}" or vice versa`
                        }];
                    }
                }
                return [];
            }
        };
    }

    /**
     * Rule: Properties should have meaningful names
     */
    static meaningfulPropertyNames(): ValidationRule {
        return {
            name: 'meaningful-property-names',
            validate: (entity: Entity | Aggregate) => {
                const errors: ValidationError[] = [];
                if ('properties' in entity && entity.properties) {
                    entity.properties.forEach(property => {
                        if (property.name.length < 2) {
                            errors.push({
                                code: 'SHORT_PROPERTY_NAME',
                                message: `Property name "${property.name}" is too short`,
                                field: property.name,
                                suggestion: 'Use descriptive property names with at least 2 characters'
                            });
                        }
                    });
                }
                return errors;
            }
        };
    }
}
