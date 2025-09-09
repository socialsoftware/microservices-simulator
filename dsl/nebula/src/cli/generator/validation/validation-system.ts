
import { Aggregate } from "../../../language/generated/ast.js";

export interface ValidationResult {
    isValid: boolean;
    errors: ValidationError[];
    warnings: ValidationWarning[];
    summary: ValidationSummary;
}

export interface ValidationError {
    type: 'error';
    code: string;
    message: string;
    file?: string;
    line?: number;
    column?: number;
    severity: 'high' | 'medium' | 'low';
}

export interface ValidationWarning {
    type: 'warning';
    code: string;
    message: string;
    file?: string;
    line?: number;
    column?: number;
    severity: 'high' | 'medium' | 'low';
}

export interface ValidationSummary {
    totalErrors: number;
    totalWarnings: number;
    highSeverityIssues: number;
    mediumSeverityIssues: number;
    lowSeverityIssues: number;
    passedChecks: number;
    failedChecks: number;
}

export interface ValidationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
    strictMode?: boolean;
    includeWarnings?: boolean;
    customRules?: ValidationRule[];
}

export interface ValidationRule {
    name: string;
    description: string;
    severity: 'error' | 'warning';
    check: (aggregate: Aggregate, options: ValidationOptions) => Promise<ValidationError[] | ValidationWarning[]>;
}

export class ValidationSystem {
    private customRules: ValidationRule[] = [];

    constructor() {
    }

    async validateAggregate(aggregate: Aggregate, options: ValidationOptions): Promise<ValidationResult> {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        try {
            if (!aggregate.name || aggregate.name.trim() === '') {
                errors.push({
                    type: 'error',
                    code: 'INVALID_AGGREGATE_NAME',
                    message: 'Aggregate name cannot be empty',
                    file: 'aggregate',
                    line: 0,
                    column: 0,
                    severity: 'high'
                });
            }

            if (options.customRules) {
                for (const rule of options.customRules) {
                    try {
                        const ruleResults = await rule.check(aggregate, options);
                        if (rule.severity === 'error') {
                            errors.push(...(ruleResults as ValidationError[]));
                        } else {
                            warnings.push(...(ruleResults as ValidationWarning[]));
                        }
                    } catch (error) {
                        errors.push({
                            type: 'error',
                            code: 'CUSTOM_RULE_ERROR',
                            message: `Custom rule '${rule.name}' failed: ${error}`,
                            severity: 'high'
                        });
                    }
                }
            }

            const builtInResults = await this.runBuiltInRules(aggregate, options);
            errors.push(...builtInResults.errors);
            warnings.push(...builtInResults.warnings);

        } catch (error) {
            errors.push({
                type: 'error',
                code: 'VALIDATION_SYSTEM_ERROR',
                message: `Validation system error: ${error}`,
                severity: 'high'
            });
        }

        const summary = this.generateSummary(errors, warnings);

        return {
            isValid: errors.length === 0,
            errors,
            warnings: options.includeWarnings !== false ? warnings : [],
            summary
        };
    }

    async validateAggregates(aggregates: Aggregate[], options: ValidationOptions): Promise<ValidationResult[]> {
        const results: ValidationResult[] = [];

        for (const aggregate of aggregates) {
            const result = await this.validateAggregate(aggregate, options);
            results.push(result);
        }

        return results;
    }

    addCustomRule(rule: ValidationRule): void {
        this.customRules.push(rule);
    }

    removeCustomRule(ruleName: string): void {
        this.customRules = this.customRules.filter(rule => rule.name !== ruleName);
    }

    getCustomRules(): ValidationRule[] {
        return [...this.customRules];
    }

    private async runBuiltInRules(aggregate: Aggregate, options: ValidationOptions): Promise<{ errors: ValidationError[], warnings: ValidationWarning[] }> {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            errors.push({
                type: 'error',
                code: 'NO_ROOT_ENTITY',
                message: `Aggregate '${aggregate.name}' must have a root entity`,
                severity: 'high'
            });
        }

        const entityNames = aggregate.entities.map((e: any) => e.name);
        const duplicateNames = entityNames.filter((name: any, index: any) => entityNames.indexOf(name) !== index);
        if (duplicateNames.length > 0) {
            errors.push({
                type: 'error',
                code: 'DUPLICATE_ENTITY_NAMES',
                message: `Duplicate entity names found: ${duplicateNames.join(', ')}`,
                severity: 'high'
            });
        }

        for (const entity of aggregate.entities) {
            if (entity.properties) {
                for (const property of entity.properties) {
                    if (!this.isValidPropertyName(property.name)) {
                        errors.push({
                            type: 'error',
                            code: 'INVALID_PROPERTY_NAME',
                            message: `Invalid property name '${property.name}' in entity '${entity.name}'`,
                            severity: 'medium'
                        });
                    }
                }
            }
        }

        if (!options.projectName || options.projectName.trim() === '') {
            errors.push({
                type: 'error',
                code: 'MISSING_PROJECT_NAME',
                message: 'Project name is required',
                severity: 'high'
            });
        }

        const validArchitectures = ['default', 'causal-saga', 'external-dto-removal'];
        if (options.architecture && !validArchitectures.includes(options.architecture)) {
            warnings.push({
                type: 'warning',
                code: 'UNKNOWN_ARCHITECTURE',
                message: `Unknown architecture '${options.architecture}', using default`,
                severity: 'medium'
            });
        }

        const validFeatures = ['events', 'validation', 'webapi', 'coordination', 'saga'];
        if (options.features) {
            const invalidFeatures = options.features.filter(f => !validFeatures.includes(f));
            if (invalidFeatures.length > 0) {
                warnings.push({
                    type: 'warning',
                    code: 'UNKNOWN_FEATURES',
                    message: `Unknown features: ${invalidFeatures.join(', ')}`,
                    severity: 'low'
                });
            }
        }

        return { errors, warnings };
    }


    private isValidPropertyName(name: string): boolean {
        const javaIdentifierRegex = /^[a-zA-Z_$][a-zA-Z0-9_$]*$/;
        return javaIdentifierRegex.test(name) && !this.isJavaReservedWord(name);
    }

    private isJavaReservedWord(name: string): boolean {
        const reservedWords = [
            'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
            'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
            'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native',
            'new', 'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp',
            'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void',
            'volatile', 'while', 'true', 'false', 'null'
        ];
        return reservedWords.includes(name.toLowerCase());
    }

    private generateSummary(errors: ValidationError[], warnings: ValidationWarning[]): ValidationSummary {
        const allIssues = [...errors, ...warnings];

        return {
            totalErrors: errors.length,
            totalWarnings: warnings.length,
            highSeverityIssues: allIssues.filter(issue => issue.severity === 'high').length,
            mediumSeverityIssues: allIssues.filter(issue => issue.severity === 'medium').length,
            lowSeverityIssues: allIssues.filter(issue => issue.severity === 'low').length,
            passedChecks: allIssues.length === 0 ? 1 : 0,
            failedChecks: allIssues.length
        };
    }


    getValidationReport(result: ValidationResult): string {
        let report = `\n=== VALIDATION REPORT ===\n`;
        report += `Status: ${result.isValid ? 'PASSED' : 'FAILED'}\n`;
        report += `Errors: ${result.summary.totalErrors}\n`;
        report += `Warnings: ${result.summary.totalWarnings}\n`;
        report += `High Severity: ${result.summary.highSeverityIssues}\n`;
        report += `Medium Severity: ${result.summary.mediumSeverityIssues}\n`;
        report += `Low Severity: ${result.summary.lowSeverityIssues}\n\n`;

        if (result.errors.length > 0) {
            report += `=== ERRORS ===\n`;
            for (const error of result.errors) {
                report += `[${error.severity.toUpperCase()}] ${error.code}: ${error.message}\n`;
                if (error.file) {
                    report += `  File: ${error.file}`;
                    if (error.line) {
                        report += `:${error.line}`;
                        if (error.column) {
                            report += `:${error.column}`;
                        }
                    }
                    report += `\n`;
                }
            }
            report += `\n`;
        }

        if (result.warnings.length > 0) {
            report += `=== WARNINGS ===\n`;
            for (const warning of result.warnings) {
                report += `[${warning.severity.toUpperCase()}] ${warning.code}: ${warning.message}\n`;
                if (warning.file) {
                    report += `  File: ${warning.file}`;
                    if (warning.line) {
                        report += `:${warning.line}`;
                        if (warning.column) {
                            report += `:${warning.column}`;
                        }
                    }
                    report += `\n`;
                }
            }
            report += `\n`;
        }

        return report;
    }
}
