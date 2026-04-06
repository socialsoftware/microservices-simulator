

export class StringUtils {
    
    
    

    

    static capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    

    static lowercase(str: string): string {
        if (!str) return '';
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

    

    static pascalCase(str: string): string {
        if (!str) return '';
        return str
            .split(/[-_\s]+/)
            .map(word => this.capitalize(word))
            .join('');
    }

    

    static camelCase(str: string): string {
        const pascal = this.pascalCase(str);
        return this.lowercase(pascal);
    }

    

    static snakeCase(str: string): string {
        if (!str) return '';
        return str
            .replace(/([A-Z])/g, '_$1')
            .toLowerCase()
            .replace(/^_/, '');
    }

    

    static kebabCase(str: string): string {
        if (!str) return '';
        return str
            .replace(/([A-Z])/g, '-$1')
            .toLowerCase()
            .replace(/^-/, '');
    }

    
    
    

    

    static pluralize(word: string): string {
        if (!word) return '';

        
        const irregulars: Record<string, string> = {
            'person': 'people',
            'child': 'children',
            'man': 'men',
            'woman': 'women',
        };

        const lowerWord = word.toLowerCase();
        if (irregulars[lowerWord]) {
            return irregulars[lowerWord];
        }

        
        if (word.endsWith('s') || word.endsWith('x') || word.endsWith('z') ||
            word.endsWith('ch') || word.endsWith('sh')) {
            return word + 'es';
        }

        if (word.endsWith('y') && !this.isVowel(word.charAt(word.length - 2))) {
            return word.slice(0, -1) + 'ies';
        }

        return word + 's';
    }

    

    private static isVowel(char: string): boolean {
        return /[aeiouAEIOU]/.test(char);
    }

    
    
    

    

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


    static indent(str: string, spaces: number = 4): string {
        const indentStr = ' '.repeat(spaces);
        return str.split('\n').map(line => line ? indentStr + line : line).join('\n');
    }

    static dedent(str: string): string {
        const lines = str.split('\n');
        if (lines.length <= 1) {
            return str;
        }

        let minIndent = Infinity;
        for (const line of lines) {
            if (line.trim().length > 0) {
                const indent = line.match(/^(\s*)/)?.[1].length || 0;
                minIndent = Math.min(minIndent, indent);
            }
        }

        return lines.map(line => {
            if (line.trim().length === 0) {
                return line;
            }
            return line.substring(minIndent);
        }).join('\n');
    }

    static normalizeWhitespace(str: string): string {
        return str.replace(/\s+/g, ' ').trim();
    }


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

    static isValidIdentifier(str: string): boolean {
        if (!str || str.length === 0) {
            return false;
        }

        if (str.startsWith('@')) {
            return /^@[a-zA-Z_$][a-zA-Z0-9_$]*$/.test(str);
        }

        if (!/^[a-zA-Z_$]/.test(str)) {
            return false;
        }

        return /^[a-zA-Z0-9_$]+$/.test(str);
    }
}


export const capitalize = StringUtils.capitalize;

export const lowercase = StringUtils.lowercase;

export const pascalCase = StringUtils.pascalCase;

export const camelCase = StringUtils.camelCase;

export const pluralize = StringUtils.pluralize;

export const escapeHtml = StringUtils.escapeHtml;

export const unescapeHtml = StringUtils.unescapeHtml;
