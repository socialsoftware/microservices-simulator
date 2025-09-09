/**
 * Utility functions for template processing
 */

import { ValidationError, ValidationWarning } from '../generator/template-engine/types.js';

export class Utils {
    /**
     * Escape HTML characters in a string
     */
    static escapeHtml(str: string): string {
        if (typeof str !== 'string') {
            return String(str);
        }

        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    /**
     * Unescape HTML characters in a string
     */
    static unescapeHtml(str: string): string {
        if (typeof str !== 'string') {
            return String(str);
        }

        return str
            .replace(/&amp;/g, '&')
            .replace(/&lt;/g, '<')
            .replace(/&gt;/g, '>')
            .replace(/&quot;/g, '"')
            .replace(/&#39;/g, "'");
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
     */
    static toString(value: any): string {
        if (value === null || value === undefined) {
            return '';
        }
        if (typeof value === 'string') {
            return value;
        }
        if (typeof value === 'number' || typeof value === 'boolean') {
            return String(value);
        }
        if (Array.isArray(value)) {
            return value.map(item => this.toString(item)).join('');
        }
        if (typeof value === 'object') {
            return JSON.stringify(value);
        }
        return String(value);
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
     */
    static isValidIdentifier(str: string): boolean {
        if (!str || str.length === 0) {
            return false;
        }

        // Special identifiers like @index, @first, @last are valid
        if (str.startsWith('@')) {
            return /^@[a-zA-Z_$][a-zA-Z0-9_$]*$/.test(str);
        }

        // Must start with letter, underscore, or dollar sign
        if (!/^[a-zA-Z_$]/.test(str)) {
            return false;
        }

        // Can contain letters, digits, underscores, and dollar signs
        return /^[a-zA-Z0-9_$]+$/.test(str);
    }

    /**
     * Normalize whitespace in a string
     */
    static normalizeWhitespace(str: string): string {
        return str.replace(/\s+/g, ' ').trim();
    }

    /**
     * Indent a string by a specified number of spaces
     */
    static indent(str: string, spaces: number): string {
        const indentStr = ' '.repeat(spaces);
        return str.split('\n').map(line => line ? indentStr + line : line).join('\n');
    }

    /**
     * Dedent a string by removing common leading whitespace
     */
    static dedent(str: string): string {
        const lines = str.split('\n');
        if (lines.length <= 1) {
            return str;
        }

        // Find the minimum indentation (excluding empty lines)
        let minIndent = Infinity;
        for (const line of lines) {
            if (line.trim().length > 0) {
                const indent = line.match(/^(\s*)/)?.[1].length || 0;
                minIndent = Math.min(minIndent, indent);
            }
        }

        // Remove the common indentation
        return lines.map(line => {
            if (line.trim().length === 0) {
                return line;
            }
            return line.substring(minIndent);
        }).join('\n');
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
     */
    static capitalize(str: string): string {
        if (!str || str.length === 0) {
            return str;
        }
        return str.charAt(0).toUpperCase() + str.slice(1);
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
export const capitalize = Utils.capitalize;

// Export resolveParamReturnType function for backward compatibility
export const resolveParamReturnType = Utils.resolveParamReturnType;
