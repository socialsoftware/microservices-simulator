import { Entity } from "../../../language/generated/ast.js";

export interface ValidationGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export interface ValidationContext {
    aggregateName: string;
    capitalizedAggregate: string;
    lowerAggregate: string;
    packageName: string;
    rootEntity: Entity;
    projectName: string;
    imports: string[];
}

export interface InvariantContext extends ValidationContext {
    rootEntityType: string;
    invariants: any[];
    invariantMethods: any[];
}

export interface AnnotationContext extends ValidationContext {
    annotations: any[];
    validationAnnotations: any[];
}

export interface ValidatorContext extends ValidationContext {
    validators: any[];
    customValidators: any[];
}

export interface ValidationRule {
    name: string;
    type: 'invariant' | 'constraint' | 'custom';
    property?: string;
    condition: string;
    message: string;
    severity: 'error' | 'warning' | 'info';
}
