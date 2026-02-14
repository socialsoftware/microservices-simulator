/**
 * Utility functions for template processing
 *
 * NOTE: String manipulation methods have been moved to string-utils.ts
 * This file now focuses on template-specific utilities.
 */

import { ValidationError, ValidationWarning } from '../generators/common/types.js';
import { StringUtils } from './string-utils.js';

export class Utils {
    /**
     * Escape HTML characters in a string
     * @deprecated Use StringUtils.escapeHtml() instead
     */
    static escapeHtml(str: string): string {
        return StringUtils.escapeHtml(str);
    }

    /**
     * Unescape HTML characters in a string
     * @deprecated Use StringUtils.unescapeHtml() instead
     */
    static unescapeHtml(str: string): string {
        return StringUtils.unescapeHtml(str);
    }

    /**
     * Get the value of a property from an object using dot notation
     */
    static getProperty(obj: any, path: string): any {
        if (!obj || !path) {
            return undefined;
        }

        const parts = path.split('.');
        let current = obj;

        for (const part of parts) {
            if (current === null || current === undefined) {
                return undefined;
            }
            current = current[part];
        }

        return current;
    }

    /**
     * Set the value of a property in an object using dot notation
     */
    static setProperty(obj: any, path: string, value: any): void {
        if (!obj || !path) {
            return;
        }

        const parts = path.split('.');
        let current = obj;

        for (let i = 0; i < parts.length - 1; i++) {
            const part = parts[i];
            if (!(part in current) || typeof current[part] !== 'object') {
                current[part] = {};
            }
            current = current[part];
        }

        current[parts[parts.length - 1]] = value;
    }

    /**
     * Check if a value is truthy in template context
     */
    static isTruthy(value: any): boolean {
        if (value === null || value === undefined) {
            return false;
        }
        if (typeof value === 'boolean') {
            return value;
        }
        if (typeof value === 'number') {
            return value !== 0;
        }
        if (typeof value === 'string') {
            return value.length > 0;
        }
        if (Array.isArray(value)) {
            return value.length > 0;
        }
        if (typeof value === 'object') {
            return Object.keys(value).length > 0;
        }
        return true;
    }

    /**
     * Check if a value is falsy in template context
     */
    static isFalsy(value: any): boolean {
        return !this.isTruthy(value);
    }

    /**
     * Convert a value to a string for template rendering
     * @deprecated Use StringUtils.toString() instead
     */
    static toString(value: any): string {
        return StringUtils.toString(value);
    }

    /**
     * Parse a template expression (e.g., "user.name" or "items.length")
     */
    static parseExpression(expression: string): { path: string; filters: string[] } {
        const parts = expression.split('|');
        const path = parts[0].trim();
        const filters = parts.slice(1).map(filter => filter.trim());

        return { path, filters };
    }

    /**
     * Apply filters to a value
     */
    static applyFilters(value: any, filters: string[]): any {
        let result = value;

        for (const filter of filters) {
            switch (filter) {
                case 'upper':
                    result = this.toString(result).toUpperCase();
                    break;
                case 'lower':
                    result = this.toString(result).toLowerCase();
                    break;
                case 'capitalize':
                    const str = this.toString(result);
                    result = str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
                    break;
                case 'escape':
                    result = this.escapeHtml(this.toString(result));
                    break;
                case 'unescape':
                    result = this.unescapeHtml(this.toString(result));
                    break;
                case 'json':
                    result = JSON.stringify(result);
                    break;
                case 'length':
                    if (Array.isArray(result) || typeof result === 'string') {
                        result = result.length;
                    } else {
                        result = 0;
                    }
                    break;
                default:
                    // Unknown filter, keep value as is
                    break;
            }
        }

        return result;
    }

    /**
     * Create a validation error
     */
    static createValidationError(
        type: 'syntax' | 'semantic' | 'reference',
        message: string,
        line?: number,
        column?: number,
        position?: number
    ): ValidationError {
        return { type, message, line, column, position };
    }

    /**
     * Create a validation warning
     */
    static createValidationWarning(
        type: 'performance' | 'style' | 'deprecation',
        message: string,
        line?: number,
        column?: number,
        position?: number
    ): ValidationWarning {
        return { type, message, line, column, position };
    }

    /**
     * Calculate line and column from position in source
     */
    static getLineColumn(source: string, position: number): { line: number; column: number } {
        let line = 1;
        let column = 1;

        for (let i = 0; i < position && i < source.length; i++) {
            if (source[i] === '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        return { line, column };
    }

    /**
     * Check if a string is a valid identifier
     * @deprecated Use StringUtils.isValidIdentifier() instead
     */
    static isValidIdentifier(str: string): boolean {
        return StringUtils.isValidIdentifier(str);
    }

    /**
     * Normalize whitespace in a string
     * @deprecated Use StringUtils.normalizeWhitespace() instead
     */
    static normalizeWhitespace(str: string): string {
        return StringUtils.normalizeWhitespace(str);
    }

    /**
     * Indent a string by a specified number of spaces
     * @deprecated Use StringUtils.indent() instead
     */
    static indent(str: string, spaces: number): string {
        return StringUtils.indent(str, spaces);
    }

    /**
     * Dedent a string by removing common leading whitespace
     * @deprecated Use StringUtils.dedent() instead
     */
    static dedent(str: string): string {
        return StringUtils.dedent(str);
    }

    /**
 * Resolve Java type from field type (delegates to TypeResolver)
 */
    static resolveJavaType(fieldType: any): string {
        // Import TypeResolver dynamically to avoid circular dependencies
        const { TypeResolver } = require('./type-resolver.js');
        return TypeResolver.resolveJavaType(fieldType);
    }

    /**
     * Capitalize first letter of a string
     * @deprecated Use StringUtils.capitalize() instead
     */
    static capitalize(str: string): string {
        return StringUtils.capitalize(str);
    }

    /**
     * Resolve parameter or return type
     */
    static resolveParamReturnType(type: any): string {
        return this.resolveJavaType(type);
    }
}

// Export the resolveJavaType function for backward compatibility
export const resolveJavaType = Utils.resolveJavaType;

// Export capitalize function for backward compatibility
// @deprecated Use StringUtils.capitalize() instead
export const capitalize = StringUtils.capitalize;

// Export resolveParamReturnType function for backward compatibility
export const resolveParamReturnType = Utils.resolveParamReturnType;
