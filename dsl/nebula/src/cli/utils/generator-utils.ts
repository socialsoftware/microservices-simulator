


import { ValidationError, ValidationWarning } from '../generators/common/types.js';
import { StringUtils } from './string-utils.js';

export class Utils {
    

    static escapeHtml(str: string): string {
        return StringUtils.escapeHtml(str);
    }

    

    static unescapeHtml(str: string): string {
        return StringUtils.unescapeHtml(str);
    }

    

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

    

    static isFalsy(value: any): boolean {
        return !this.isTruthy(value);
    }

    

    static toString(value: any): string {
        return StringUtils.toString(value);
    }

    

    static parseExpression(expression: string): { path: string; filters: string[] } {
        const parts = expression.split('|');
        const path = parts[0].trim();
        const filters = parts.slice(1).map(filter => filter.trim());

        return { path, filters };
    }

    

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
                    
                    break;
            }
        }

        return result;
    }

    

    static createValidationError(
        type: 'syntax' | 'semantic' | 'reference',
        message: string,
        line?: number,
        column?: number,
        position?: number
    ): ValidationError {
        return { type, message, line, column, position };
    }

    

    static createValidationWarning(
        type: 'performance' | 'style' | 'deprecation',
        message: string,
        line?: number,
        column?: number,
        position?: number
    ): ValidationWarning {
        return { type, message, line, column, position };
    }

    

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

    

    static isValidIdentifier(str: string): boolean {
        return StringUtils.isValidIdentifier(str);
    }

    

    static normalizeWhitespace(str: string): string {
        return StringUtils.normalizeWhitespace(str);
    }

    

    static indent(str: string, spaces: number): string {
        return StringUtils.indent(str, spaces);
    }

    

    static dedent(str: string): string {
        return StringUtils.dedent(str);
    }

    

    static resolveJavaType(fieldType: any): string {
        
        const { TypeResolver } = require('./type-resolver.js');
        return TypeResolver.resolveJavaType(fieldType);
    }

    

    static capitalize(str: string): string {
        return StringUtils.capitalize(str);
    }

    

    static resolveParamReturnType(type: any): string {
        return this.resolveJavaType(type);
    }
}


export const resolveJavaType = Utils.resolveJavaType;



export const capitalize = StringUtils.capitalize;


export const resolveParamReturnType = Utils.resolveParamReturnType;
