/**
 * Simplified template engine for code generation
 * Focuses on core functionality: variable substitution, conditionals, and loops
 */

export interface SimpleTemplateContext {
    [key: string]: any;
}

export interface SimpleTemplateOptions {
    strictMode?: boolean;
    maxDepth?: number;
}

export class SimpleTemplateEngine {
    private options: SimpleTemplateOptions;

    constructor(options: SimpleTemplateOptions = {}) {
        this.options = {
            strictMode: false,
            maxDepth: 10,
            ...options
        };
    }

    /**
     * Render a template with the given context
     */
    render(template: string, context: SimpleTemplateContext): string {
        try {
            return this.processTemplate(template, context, 0);
        } catch (error) {
            console.error('Template rendering error:', error);
            throw error;
        }
    }

    /**
     * Process template with variable substitution, conditionals, and loops
     */
    private processTemplate(template: string, context: SimpleTemplateContext, depth: number): string {
        if (depth > this.options.maxDepth!) {
            throw new Error(`Template depth exceeded maximum of ${this.options.maxDepth}`);
        }

        let result = template;

        // Handle variable substitution: {{variable}}
        result = result.replace(/\{\{([^}]+)\}\}/g, (match, expression) => {
            const value = this.evaluateExpression(expression.trim(), context);
            return value !== null && value !== undefined ? String(value) : '';
        });

        // Handle conditionals: {{#if condition}}...{{/if}}
        result = this.processConditionals(result, context, depth);

        // Handle loops: {{#each array}}...{{/each}}
        result = this.processLoops(result, context, depth);

        return result;
    }

    /**
     * Process conditional blocks
     */
    private processConditionals(template: string, context: SimpleTemplateContext, depth: number): string {
        const ifRegex = /\{\{#if\s+([^}]+)\}\}([\s\S]*?)\{\{\/if\}\}/g;

        return template.replace(ifRegex, (match, condition, content) => {
            const conditionValue = this.evaluateExpression(condition.trim(), context);
            const isTruthy = this.isTruthy(conditionValue);

            if (isTruthy) {
                return this.processTemplate(content, context, depth + 1);
            }
            return '';
        });
    }

    /**
     * Process loop blocks
     */
    private processLoops(template: string, context: SimpleTemplateContext, depth: number): string {
        const eachRegex = /\{\{#each\s+([^}]+)\}\}([\s\S]*?)\{\{\/each\}\}/g;

        return template.replace(eachRegex, (match, arrayExpression, content) => {
            const array = this.evaluateExpression(arrayExpression.trim(), context);

            if (!Array.isArray(array)) {
                if (this.options.strictMode) {
                    throw new Error(`Expected array for {{#each ${arrayExpression}}}, got ${typeof array}`);
                }
                return '';
            }

            return array.map((item, index) => {
                const itemContext = {
                    ...context,
                    this: item,
                    index: index,
                    first: index === 0,
                    last: index === array.length - 1
                };
                return this.processTemplate(content, itemContext, depth + 1);
            }).join('');
        });
    }

    /**
     * Evaluate a simple expression
     */
    private evaluateExpression(expression: string, context: SimpleTemplateContext): any {
        // Handle simple property access: property.subproperty
        if (expression.includes('.')) {
            const parts = expression.split('.');
            let value = context;

            for (const part of parts) {
                if (value && typeof value === 'object' && part in value) {
                    value = value[part];
                } else {
                    return undefined;
                }
            }
            return value;
        }

        // Handle simple variable access
        if (expression in context) {
            return context[expression];
        }

        // Handle string literals
        if (expression.startsWith('"') && expression.endsWith('"')) {
            return expression.slice(1, -1);
        }

        if (expression.startsWith("'") && expression.endsWith("'")) {
            return expression.slice(1, -1);
        }

        // Handle boolean literals
        if (expression === 'true') return true;
        if (expression === 'false') return false;

        // Handle null/undefined
        if (expression === 'null') return null;
        if (expression === 'undefined') return undefined;

        return undefined;
    }

    /**
     * Check if a value is truthy
     */
    private isTruthy(value: any): boolean {
        if (value === null || value === undefined) return false;
        if (typeof value === 'boolean') return value;
        if (typeof value === 'string') return value.length > 0;
        if (typeof value === 'number') return value !== 0;
        if (Array.isArray(value)) return value.length > 0;
        if (typeof value === 'object') return Object.keys(value).length > 0;
        return true;
    }

    /**
     * Register a helper function
     */
    registerHelper(name: string, helper: (value: any, options: any) => string): void {
        // For now, we'll implement this as a simple extension
        // In a more complex implementation, this would be integrated into the expression evaluator
        console.warn(`Helper registration not yet implemented: ${name}`);
    }

    /**
     * Clear any cached templates
     */
    clearCache(): void {
        // Simple engine doesn't use caching
    }
}
