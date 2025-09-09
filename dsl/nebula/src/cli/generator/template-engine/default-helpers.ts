import { TemplateHelper, HelperOptions } from './types.js';

export class DefaultHelpers {
    static registerAll(registerFn: (name: string, helper: TemplateHelper) => void): void {
        registerFn('eq', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a === b ? options.fn({}) : options.inverse({});
        });

        registerFn('ne', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a !== b ? options.fn({}) : options.inverse({});
        });

        registerFn('gt', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a > b ? options.fn({}) : options.inverse({});
        });

        registerFn('lt', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a < b ? options.fn({}) : options.inverse({});
        });

        registerFn('ge', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a >= b ? options.fn({}) : options.inverse({});
        });

        registerFn('le', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a <= b ? options.fn({}) : options.inverse({});
        });

        registerFn('and', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a && b ? options.fn({}) : options.inverse({});
        });

        registerFn('or', (a: any, options: HelperOptions) => {
            const b = options.hash.b;
            return a || b ? options.fn({}) : options.inverse({});
        });

        registerFn('not', (a: any, options: HelperOptions) => {
            return !a ? options.fn({}) : options.inverse({});
        });

        registerFn('default', (value: any, options: HelperOptions) => {
            const defaultValue = options.hash.default || '';
            return value || defaultValue;
        });

        registerFn('join', (array: any, options: HelperOptions) => {
            const separator = options.hash.separator || ', ';
            if (Array.isArray(array)) {
                return array.join(separator);
            }
            return '';
        });

        registerFn('length', (value: any, options: HelperOptions) => {
            if (Array.isArray(value) || typeof value === 'string') {
                return value.length.toString();
            }
            return '0';
        });

        registerFn('first', (array: any, options: HelperOptions) => {
            if (Array.isArray(array) && array.length > 0) {
                return array[0];
            }
            return '';
        });

        registerFn('last', (array: any, options: HelperOptions) => {
            if (Array.isArray(array) && array.length > 0) {
                return array[array.length - 1];
            }
            return '';
        });

        registerFn('capitalize', (str: any, options: HelperOptions) => {
            if (typeof str === 'string' && str.length > 0) {
                return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
            }
            return str;
        });

        registerFn('upper', (str: any, options: HelperOptions) => {
            return typeof str === 'string' ? str.toUpperCase() : str;
        });

        registerFn('lower', (str: any, options: HelperOptions) => {
            return typeof str === 'string' ? str.toLowerCase() : str;
        });

        registerFn('camelCase', (str: any, options: HelperOptions) => {
            if (typeof str !== 'string') return str;
            return str.replace(/[-_\s]+(.)?/g, (_, char) => char ? char.toUpperCase() : '');
        });

        registerFn('pascalCase', (str: any, options: HelperOptions) => {
            if (typeof str !== 'string') return str;
            const camelCase = str.replace(/[-_\s]+(.)?/g, (_, char) => char ? char.toUpperCase() : '');
            return camelCase.charAt(0).toUpperCase() + camelCase.slice(1);
        });

        registerFn('kebabCase', (str: any, options: HelperOptions) => {
            if (typeof str !== 'string') return str;
            return str.replace(/([A-Z])/g, '-$1').replace(/[-_\s]+/g, '-').toLowerCase().replace(/^-/, '');
        });

        registerFn('snakeCase', (str: any, options: HelperOptions) => {
            if (typeof str !== 'string') return str;
            return str.replace(/([A-Z])/g, '_$1').replace(/[-_\s]+/g, '_').toLowerCase().replace(/^_/, '');
        });

        registerFn('pluralize', (str: any, options: HelperOptions) => {
            if (typeof str !== 'string') return str;
            if (str.endsWith('y')) {
                return str.slice(0, -1) + 'ies';
            }
            if (str.endsWith('s') || str.endsWith('sh') || str.endsWith('ch') || str.endsWith('x') || str.endsWith('z')) {
                return str + 'es';
            }
            return str + 's';
        });

        registerFn('singularize', (str: any, options: HelperOptions) => {
            if (typeof str !== 'string') return str;
            if (str.endsWith('ies')) {
                return str.slice(0, -3) + 'y';
            }
            if (str.endsWith('es')) {
                return str.slice(0, -2);
            }
            if (str.endsWith('s') && !str.endsWith('ss')) {
                return str.slice(0, -1);
            }
            return str;
        });

        registerFn('json', (value: any, options: HelperOptions) => {
            try {
                return JSON.stringify(value, null, 2);
            } catch {
                return '';
            }
        });

        registerFn('formatDate', (date: any, options: HelperOptions) => {
            const format = options.hash.format || 'YYYY-MM-DD';
            if (!date) return '';

            const d = new Date(date);
            if (isNaN(d.getTime())) return '';

            const year = d.getFullYear();
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const day = String(d.getDate()).padStart(2, '0');
            const hours = String(d.getHours()).padStart(2, '0');
            const minutes = String(d.getMinutes()).padStart(2, '0');
            const seconds = String(d.getSeconds()).padStart(2, '0');

            return format
                .replace('YYYY', String(year))
                .replace('MM', month)
                .replace('DD', day)
                .replace('HH', hours)
                .replace('mm', minutes)
                .replace('ss', seconds);
        });

        registerFn('formatNumber', (num: any, options: HelperOptions) => {
            const decimals = options.hash.decimals || 2;
            const n = parseFloat(num);
            return isNaN(n) ? '' : n.toFixed(decimals);
        });

        registerFn('add', (a: any, options: HelperOptions) => {
            const b = options.hash.b || 0;
            return ((parseFloat(a) || 0) + (parseFloat(b) || 0)).toString();
        });

        registerFn('subtract', (a: any, options: HelperOptions) => {
            const b = options.hash.b || 0;
            return ((parseFloat(a) || 0) - (parseFloat(b) || 0)).toString();
        });

        registerFn('multiply', (a: any, options: HelperOptions) => {
            const b = options.hash.b || 1;
            return ((parseFloat(a) || 0) * (parseFloat(b) || 1)).toString();
        });

        registerFn('divide', (a: any, options: HelperOptions) => {
            const b = options.hash.b || 1;
            const divisor = parseFloat(b) || 1;
            return ((parseFloat(a) || 0) / divisor).toString();
        });

        registerFn('ifCond', (a: any, options: HelperOptions) => {
            const operator = options.hash.operator || '==';
            const b = options.hash.b;

            let result = false;
            switch (operator) {
                case '==':
                    result = a == b;
                    break;
                case '===':
                    result = a === b;
                    break;
                case '!=':
                    result = a != b;
                    break;
                case '!==':
                    result = a !== b;
                    break;
                case '<':
                    result = a < b;
                    break;
                case '<=':
                    result = a <= b;
                    break;
                case '>':
                    result = a > b;
                    break;
                case '>=':
                    result = a >= b;
                    break;
                default:
                    result = false;
            }

            return result ? options.fn({}) : options.inverse({});
        });

        registerFn('contains', (array: any, options: HelperOptions) => {
            const value = options.hash.value;
            if (Array.isArray(array)) {
                return array.includes(value) ? options.fn({}) : options.inverse({});
            }
            if (typeof array === 'object' && array !== null) {
                return Object.values(array).includes(value) ? options.fn({}) : options.inverse({});
            }
            return options.inverse({});
        });

        registerFn('keys', (obj: any, options: HelperOptions) => {
            if (obj && typeof obj === 'object') {
                return Object.keys(obj).join(', ');
            }
            return '';
        });

        registerFn('values', (obj: any, options: HelperOptions) => {
            if (obj && typeof obj === 'object') {
                return Object.values(obj).join(', ');
            }
            return '';
        });

        registerFn('debug', (value: any, options: HelperOptions) => {
            console.log('Template Debug:', value);
            return JSON.stringify(value, null, 2);
        });

        registerFn('log', (value: any, options: HelperOptions) => {
            console.log('Template Log:', value);
            return '';
        });
    }
}
