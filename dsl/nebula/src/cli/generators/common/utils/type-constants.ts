/**
 * Type Constants - Centralized type classifications
 *
 * This module provides centralized constants for type checking across the codebase.
 * Replaces 11+ duplicate primitive type arrays scattered across generators.
 */

/**
 * Java primitive types used in the DSL
 */
export const JAVA_PRIMITIVE_TYPES = [
    'String',
    'Integer',
    'Long',
    'Boolean',
    'Double',
    'Float'
] as const;

/**
 * Temporal types (date/time types)
 */
export const TEMPORAL_TYPES = [
    'LocalDateTime',
    'LocalDate'
] as const;

/**
 * Additional special types
 */
export const SPECIAL_TYPES = [
    'BigDecimal',
    'void',
    'UnitOfWork'
] as const;

/**
 * Java primitive type variants (lowercase versions)
 */
export const JAVA_PRIMITIVE_VARIANTS = [
    'boolean',
    'int',
    'long',
    'double',
    'float'
] as const;

/**
 * Legacy date types (for backward compatibility)
 */
export const LEGACY_DATE_TYPES = [
    'Date',
    'DateTime'
] as const;

/**
 * All primitive types (most commonly used)
 */
export const PRIMITIVE_TYPES = [
    ...JAVA_PRIMITIVE_TYPES,
    ...TEMPORAL_TYPES,
    ...SPECIAL_TYPES
] as const;

/**
 * Extended primitive types (includes lowercase variants)
 */
export const EXTENDED_PRIMITIVE_TYPES = [
    ...PRIMITIVE_TYPES,
    ...JAVA_PRIMITIVE_VARIANTS
] as const;

/**
 * All primitive types including legacy date types
 */
export const ALL_PRIMITIVE_TYPES = [
    ...EXTENDED_PRIMITIVE_TYPES,
    ...LEGACY_DATE_TYPES
] as const;

/**
 * Check if a type is a primitive type
 * @param type The type to check
 * @returns true if the type is a primitive type
 */
export function isPrimitiveType(type: string): boolean {
    return PRIMITIVE_TYPES.includes(type as any);
}

/**
 * Check if a type is an extended primitive (includes lowercase variants)
 * @param type The type to check
 * @returns true if the type is an extended primitive type
 */
export function isExtendedPrimitiveType(type: string): boolean {
    return EXTENDED_PRIMITIVE_TYPES.includes(type as any);
}

/**
 * Check if a type is a temporal type
 * @param type The type to check
 * @returns true if the type is a temporal type
 */
export function isTemporalType(type: string): boolean {
    return TEMPORAL_TYPES.includes(type as any);
}

/**
 * Type guard for primitive type
 */
export type PrimitiveType = typeof PRIMITIVE_TYPES[number];

/**
 * Type guard for extended primitive type
 */
export type ExtendedPrimitiveType = typeof EXTENDED_PRIMITIVE_TYPES[number];
