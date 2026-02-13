/**
 * Type Extractor - Centralized type extraction utilities
 *
 * This module provides utilities for extracting enum types, DTO types, and
 * collection element types from type strings and generated code.
 * Replaces duplicate extraction logic across 3+ files.
 */

import { PRIMITIVE_TYPES } from './type-constants.js';

/**
 * Centralized type extraction utilities
 */
export class TypeExtractor {
    /**
     * Extract enum type from a type string (if it's not a primitive or DTO)
     *
     * @param type Type string (e.g., "UserRole", "List<UserRole>", "String", "UserDto")
     * @returns Enum type name if found, null otherwise
     *
     * @example
     * extractEnumType("UserRole") → "UserRole"
     * extractEnumType("List<UserRole>") → "UserRole"
     * extractEnumType("String") → null (primitive)
     * extractEnumType("UserDto") → null (DTO)
     */
    static extractEnumType(type: string): string | null {
        if (!type) return null;

        // Remove collection wrappers (List<>, Set<>)
        const typeName = type.replace(/List<|Set<|Optional<|>/g, '').trim();

        // Skip empty strings
        if (!typeName) return null;

        // Skip primitive types
        if (PRIMITIVE_TYPES.includes(typeName as any)) return null;

        // Skip DTOs
        if (typeName.endsWith('Dto')) return null;

        // Skip types that still have generic markers
        if (typeName.includes('<')) return null;

        // Only consider types starting with uppercase (enum convention)
        if (typeName.charAt(0) !== typeName.charAt(0).toUpperCase()) return null;

        return typeName;
    }

    /**
     * Extract all enum types from a type string and add to a set
     * Useful for building import lists
     *
     * @param type Type string
     * @param enumSet Set to add found enum types to
     */
    static extractEnumTypes(type: string, enumSet: Set<string>): void {
        const enumType = this.extractEnumType(type);
        if (enumType) {
            enumSet.add(enumType);
        }
    }

    /**
     * Extract enum types from generated Java code using pattern matching
     * This is useful when analyzing already-generated code
     *
     * @param javaCode Generated Java code
     * @param excludedEnums Enum names to exclude (e.g., JPA enums)
     * @returns Set of enum type names found in the code
     *
     * @example
     * extractEnumsFromCode("private UserRole role;") → Set(["UserRole"])
     */
    static extractEnumsFromCode(
        javaCode: string,
        excludedEnums: string[] = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState', 'LocalDateTime']
    ): Set<string> {
        // Pattern matches: UserRole, UserType, UserState, etc.
        const enumPattern = /\b([A-Z][a-zA-Z]*(?:Type|Role|State))\b/g;
        const foundEnums = new Set<string>();
        let match;

        while ((match = enumPattern.exec(javaCode)) !== null) {
            const enumType = match[1];

            // Skip excluded enums
            if (excludedEnums.includes(enumType)) continue;

            // Skip DTOs
            if (enumType.endsWith('Dto')) continue;

            // Skip collection types
            if (enumType.includes('List') || enumType.includes('Set')) continue;

            foundEnums.add(enumType);
        }

        return foundEnums;
    }

    /**
     * Extract DTO types from generated Java code
     *
     * @param javaCode Generated Java code
     * @returns Set of DTO type names found in the code
     *
     * @example
     * extractDtosFromCode("UserDto user;") → Set(["UserDto"])
     */
    static extractDtosFromCode(javaCode: string): Set<string> {
        const dtoPattern = /(\w+Dto)\b/g;
        const dtos = new Set<string>();
        let match;

        while ((match = dtoPattern.exec(javaCode)) !== null) {
            dtos.add(match[1]);
        }

        return dtos;
    }

    /**
     * Extract the element type from a collection type
     *
     * @param type Collection type string (e.g., "List<User>", "Set<String>")
     * @returns Element type if collection, null otherwise
     *
     * @example
     * extractCollectionElementType("List<User>") → "User"
     * extractCollectionElementType("Set<String>") → "String"
     * extractCollectionElementType("String") → null
     */
    static extractCollectionElementType(type: string): string | null {
        if (!type) return null;

        // Match List<T>, Set<T>, Optional<T>
        const match = type.match(/(?:List|Set|Optional)<(.+)>/);
        return match ? match[1].trim() : null;
    }

    /**
     * Check if a type is a collection type
     *
     * @param type Type string
     * @returns true if type is List, Set, or Optional
     */
    static isCollectionType(type: string): boolean {
        if (!type) return false;
        return type.startsWith('List<') || type.startsWith('Set<') || type.startsWith('Optional<');
    }

    /**
     * Check if a type is a DTO type
     *
     * @param type Type string
     * @returns true if type ends with 'Dto'
     */
    static isDtoType(type: string): boolean {
        if (!type) return false;
        return type.endsWith('Dto');
    }

    /**
     * Extract all types from a list and categorize them
     * Useful for building comprehensive import lists
     *
     * @param types Array of type strings
     * @returns Object with categorized types
     */
    static categorizeTypes(types: string[]): {
        enums: Set<string>;
        dtos: Set<string>;
        primitives: Set<string>;
        collections: Set<string>;
    } {
        const enums = new Set<string>();
        const dtos = new Set<string>();
        const primitives = new Set<string>();
        const collections = new Set<string>();

        for (const type of types) {
            if (!type) continue;

            if (this.isCollectionType(type)) {
                collections.add(type);
                // Also extract element type
                const elementType = this.extractCollectionElementType(type);
                if (elementType) {
                    types.push(elementType);  // Process element type
                }
            } else if (this.isDtoType(type)) {
                dtos.add(type);
            } else if (PRIMITIVE_TYPES.includes(type as any)) {
                primitives.add(type);
            } else {
                const enumType = this.extractEnumType(type);
                if (enumType) {
                    enums.add(enumType);
                }
            }
        }

        return { enums, dtos, primitives, collections };
    }
}
