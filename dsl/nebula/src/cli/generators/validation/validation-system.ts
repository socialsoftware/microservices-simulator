/**
 * Simplified Validation System
 *
 * This validation system checks only for duplicate aggregate names across the project.
 * All other validation (syntax, semantics, types) is handled by Langium's LSP automatically.
 *
 * Rationale: Langium provides comprehensive validation through its language server.
 * Runtime validation during code generation should be minimal and focus only on
 * cross-file concerns that Langium cannot detect (like duplicate names across files).
 */

import { Aggregate } from "../../../language/generated/ast.js";

/**
 * Validation result
 */
export interface ValidationResult {
    isValid: boolean;
    errors: ValidationError[];
    warnings: ValidationWarning[];
}

/**
 * Validation error
 */
export interface ValidationError {
    code: string;
    message: string;
    aggregateName?: string;
    severity: 'error' | 'warning';
}

/**
 * Validation warning
 */
export interface ValidationWarning {
    code: string;
    message: string;
    aggregateName?: string;
    severity: 'error' | 'warning';
}

/**
 * Validation options
 */
export interface ValidationOptions {
    projectName: string;
}

/**
 * Minimal Aggregate Validator
 *
 * Validates only cross-file concerns that Langium cannot detect.
 * Currently checks: duplicate aggregate names across all files.
 */
export class AggregateValidator {

    /**
     * Validate a single aggregate (kept for backwards compatibility)
     *
     * Note: This method cannot detect duplicate names across files.
     * Use validateAggregates() for complete validation.
     */
    async validateAggregate(aggregate: Aggregate, options: ValidationOptions): Promise<ValidationResult> {
        // Single aggregate validation - no cross-file checks possible
        // Return valid result (Langium handles syntax/semantic validation)
        return {
            isValid: true,
            errors: [],
            warnings: []
        };
    }

    /**
     * Validate multiple aggregates (checks for duplicate names)
     *
     * This is the primary validation method that should be used.
     * Checks for duplicate aggregate names across all files.
     */
    async validateAggregates(aggregates: Aggregate[], options: ValidationOptions): Promise<ValidationResult> {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        // Check for duplicate aggregate names
        const aggregateNames = new Map<string, number>();

        for (const aggregate of aggregates) {
            const name = aggregate.name;
            const count = aggregateNames.get(name) || 0;
            aggregateNames.set(name, count + 1);
        }

        // Report duplicates
        for (const [name, count] of aggregateNames.entries()) {
            if (count > 1) {
                errors.push({
                    code: 'DUPLICATE_AGGREGATE_NAME',
                    message: `Duplicate aggregate name found: '${name}' appears ${count} times. Each aggregate must have a unique name.`,
                    aggregateName: name,
                    severity: 'error'
                });
            }
        }

        return {
            isValid: errors.length === 0,
            errors,
            warnings
        };
    }

    /**
     * Generate validation report for display
     */
    getValidationReport(result: ValidationResult): string {
        let report = `\n=== VALIDATION REPORT ===\n`;
        report += `Status: ${result.isValid ? '✅ PASSED' : '❌ FAILED'}\n`;
        report += `Errors: ${result.errors.length}\n`;
        report += `Warnings: ${result.warnings.length}\n\n`;

        if (result.errors.length > 0) {
            report += `=== ERRORS ===\n`;
            for (const error of result.errors) {
                report += `[${error.code}] ${error.message}\n`;
                if (error.aggregateName) {
                    report += `  Aggregate: ${error.aggregateName}\n`;
                }
            }
            report += `\n`;
        }

        if (result.warnings.length > 0) {
            report += `=== WARNINGS ===\n`;
            for (const warning of result.warnings) {
                report += `[${warning.code}] ${warning.message}\n`;
                if (warning.aggregateName) {
                    report += `  Aggregate: ${warning.aggregateName}\n`;
                }
            }
            report += `\n`;
        }

        return report;
    }
}
