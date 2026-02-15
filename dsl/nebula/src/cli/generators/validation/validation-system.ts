


import { Aggregate } from "../../../language/generated/ast.js";



export interface ValidationResult {
    isValid: boolean;
    errors: ValidationError[];
    warnings: ValidationWarning[];
}



export interface ValidationError {
    code: string;
    message: string;
    aggregateName?: string;
    severity: 'error' | 'warning';
}



export interface ValidationWarning {
    code: string;
    message: string;
    aggregateName?: string;
    severity: 'error' | 'warning';
}



export interface ValidationOptions {
    projectName: string;
}



export class AggregateValidator {

    

    async validateAggregate(aggregate: Aggregate, options: ValidationOptions): Promise<ValidationResult> {
        
        
        return {
            isValid: true,
            errors: [],
            warnings: []
        };
    }

    

    async validateAggregates(aggregates: Aggregate[], options: ValidationOptions): Promise<ValidationResult> {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        
        const aggregateNames = new Map<string, number>();

        for (const aggregate of aggregates) {
            const name = aggregate.name;
            const count = aggregateNames.get(name) || 0;
            aggregateNames.set(name, count + 1);
        }

        
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
