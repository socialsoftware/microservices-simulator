/**
 * Shared string utility functions to avoid duplication across generators
 */
export class StringUtils {
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
     */
    static camelCase(str: string): string {
        const pascal = this.pascalCase(str);
        return this.lowercase(pascal);
    }

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
}
