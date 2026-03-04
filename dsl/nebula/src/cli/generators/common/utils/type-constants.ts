




export const JAVA_PRIMITIVE_TYPES = [
    'String',
    'Integer',
    'Long',
    'Boolean',
    'Double',
    'Float'
] as const;



export const TEMPORAL_TYPES = [
    'LocalDateTime',
    'LocalDate'
] as const;



export const SPECIAL_TYPES = [
    'BigDecimal',
    'void',
    'UnitOfWork'
] as const;



export const JAVA_PRIMITIVE_VARIANTS = [
    'boolean',
    'int',
    'long',
    'double',
    'float'
] as const;



export const LEGACY_DATE_TYPES = [
    'Date',
    'DateTime'
] as const;



export const PRIMITIVE_TYPES = [
    ...JAVA_PRIMITIVE_TYPES,
    ...TEMPORAL_TYPES,
    ...SPECIAL_TYPES
] as const;



export const EXTENDED_PRIMITIVE_TYPES = [
    ...PRIMITIVE_TYPES,
    ...JAVA_PRIMITIVE_VARIANTS
] as const;



export const ALL_PRIMITIVE_TYPES = [
    ...EXTENDED_PRIMITIVE_TYPES,
    ...LEGACY_DATE_TYPES
] as const;



export function isPrimitiveType(type: string): boolean {
    return PRIMITIVE_TYPES.includes(type as any);
}



export function isExtendedPrimitiveType(type: string): boolean {
    return EXTENDED_PRIMITIVE_TYPES.includes(type as any);
}



export function isTemporalType(type: string): boolean {
    return TEMPORAL_TYPES.includes(type as any);
}



export type PrimitiveType = typeof PRIMITIVE_TYPES[number];



export type ExtendedPrimitiveType = typeof EXTENDED_PRIMITIVE_TYPES[number];
