/**
 * Validation Type Definitions
 *
 * Simple type definitions for validation results and options.
 * Most validation is handled by Langium's LSP automatically.
 */

import { Entity } from "../../../language/generated/ast.js";

/**
 * Options for code generation validation
 */
export interface ValidationGenerationOptions {
    architecture?: string;
    projectName: string;
}

/**
 * Basic validation context (used by generators)
 */
export interface ValidationContext {
    aggregateName: string;
    capitalizedAggregate: string;
    lowerAggregate: string;
    packageName: string;
    rootEntity: Entity;
    projectName: string;
    imports: string[];
}

/**
 * Context for invariant code generation
 */
export interface InvariantContext extends ValidationContext {
    rootEntityType: string;
    invariants: any[];
    invariantMethods: any[];
}

/**
 * Context for annotation code generation
 */
export interface AnnotationContext extends ValidationContext {
    annotations: any[];
    validationAnnotations: any[];
}

/**
 * Context for custom validator code generation
 */
export interface ValidatorContext extends ValidationContext {
    validators: any[];
    customValidators: any[];
}
