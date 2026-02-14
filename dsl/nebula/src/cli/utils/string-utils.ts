/**
 * Comprehensive string utility functions to avoid duplication across generators
 *
 * This module consolidates all string manipulation utilities into a single location.
 * Includes case conversion, HTML escaping, indentation, and template utilities.
 */
export class StringUtils {
    // =========================================================================
    // CASE CONVERSION
    // =========================================================================

    /**
     * Capitalizes the first letter of a string
     * @example capitalize("hello") => "Hello"
     */
    static capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    /**
     * Lowercases the first letter of a string
     * @example lowercase("Hello") => "hello"
     */
    static lowercase(str: string): string {
        if (!str) return '';
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

    /**
     * Converts a string to PascalCase
     * @example pascalCase("hello-world") => "HelloWorld"
     * @example pascalCase("hello_world") => "HelloWorld"
     */
    static pascalCase(str: string): string {
        if (!str) return '';
        return str
            .split(/[-_\s]+/)
            .map(word => this.capitalize(word))
            .join('');
    }

    /**
     * Converts a string to camelCase
     * @example camelCase("hello-world") => "helloWorld"
     * @example camelCase("hello_world") => "helloWorld"
     */
    static camelCase(str: string): string {
        const pascal = this.pascalCase(str);
        return this.lowercase(pascal);
    }

    /**
     * Converts a string to snake_case
     * @example snakeCase("helloWorld") => "hello_world"
     * @example snakeCase("HelloWorld") => "hello_world"
     */
    static snakeCase(str: string): string {
        if (!str) return '';
        return str
            .replace(/([A-Z])/g, '_$1')
            .toLowerCase()
            .replace(/^_/, '');
    }

    /**
     * Converts a string to kebab-case
     * @example kebabCase("helloWorld") => "hello-world"
     * @example kebabCase("HelloWorld") => "hello-world"
     */
    static kebabCase(str: string): string {
        if (!str) return '';
        return str
            .replace(/([A-Z])/g, '-$1')
            .toLowerCase()
            .replace(/^-/, '');
    }

    // =========================================================================
    // PLURALIZATION
    // =========================================================================

    /**
     * Pluralizes a word (simple English rules)
     * @example pluralize("user") => "users"
     * @example pluralize("class") => "classes"
     */
    static pluralize(word: string): string {
        if (!word) return '';

        // Special cases
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

        // Regular rules
        if (word.endsWith('s') || word.endsWith('x') || word.endsWith('z') ||
            word.endsWith('ch') || word.endsWith('sh')) {
            return word + 'es';
        }

        if (word.endsWith('y') && !this.isVowel(word.charAt(word.length - 2))) {
            return word.slice(0, -1) + 'ies';
        }

        return word + 's';
    }

    /**
     * Checks if a character is a vowel
     */
    private static isVowel(char: string): boolean {
        return /[aeiouAEIOU]/.test(char);
    }

    // =========================================================================
    // HTML ESCAPING (from generator-utils.ts)
    // =========================================================================

    /**
     * Escape HTML characters in a string
     * @example escapeHtml("<div>") => "&lt;div&gt;"
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
     * @example unescapeHtml("&lt;div&gt;") => "<div>"
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

    // =========================================================================
    // INDENTATION (from generator-utils.ts)
    // =========================================================================

    /**
     * Indent a string by a specified number of spaces
     * @example indent("hello", 4) => "    hello"
     */
    static indent(str: string, spaces: number = 4): string {
        const indentStr = ' '.repeat(spaces);
        return str.split('\n').map(line => line ? indentStr + line : line).join('\n');
    }

    /**
     * Dedent a string by removing common leading whitespace
     * @example dedent("  hello\n  world") => "hello\nworld"
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
     * Normalize whitespace in a string
     * @example normalizeWhitespace("hello   world") => "hello world"
     */
    static normalizeWhitespace(str: string): string {
        return str.replace(/\s+/g, ' ').trim();
    }

    // =========================================================================
    // STRING UTILITIES
    // =========================================================================

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
     * Check if a string is a valid identifier
     * @example isValidIdentifier("myVar") => true
     * @example isValidIdentifier("123invalid") => false
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
}

// =========================================================================
// BACKWARD COMPATIBILITY EXPORTS
// =========================================================================

/**
 * @deprecated Use StringUtils.capitalize() instead
 */
export const capitalize = StringUtils.capitalize;

/**
 * @deprecated Use StringUtils.lowercase() instead
 */
export const lowercase = StringUtils.lowercase;

/**
 * @deprecated Use StringUtils.pascalCase() instead
 */
export const pascalCase = StringUtils.pascalCase;

/**
 * @deprecated Use StringUtils.camelCase() instead
 */
export const camelCase = StringUtils.camelCase;

/**
 * @deprecated Use StringUtils.pluralize() instead
 */
export const pluralize = StringUtils.pluralize;

/**
 * @deprecated Use StringUtils.escapeHtml() instead
 */
export const escapeHtml = StringUtils.escapeHtml;

/**
 * @deprecated Use StringUtils.unescapeHtml() instead
 */
export const unescapeHtml = StringUtils.unescapeHtml;
