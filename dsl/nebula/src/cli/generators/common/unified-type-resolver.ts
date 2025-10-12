/**
 * Unified Type Resolution System
 * 
 * This module consolidates all type resolution logic from across the codebase,
 * providing consistent type handling for different contexts (entities, DTOs, WebAPI, etc.)
 */

export interface ResolvedType {
    javaType: string;
    isCollection: boolean;
    elementType?: string;
    isPrimitive: boolean;
    isEntity: boolean;
    isBuiltin: boolean;
    isDtoCandidate: boolean;
}

export interface TypeResolutionContext {
    convertToDtos?: boolean;
    targetContext?: 'entity' | 'dto' | 'webapi' | 'service' | 'repository';
    projectName?: string;
}

/**
 * Unified type resolver that handles all type resolution scenarios across the DSL system.
 * Replaces scattered type resolution logic with a single, consistent implementation.
 */
export class UnifiedTypeResolver {

    /**
     * Main type resolution method - resolves any type to Java type string
     */
    static resolve(type: any, context: TypeResolutionContext = {}): string {
        const resolved = this.resolveDetailed(type, context);
        return resolved.javaType;
    }

    /**
     * Detailed type resolution that returns full type information
     */
    static resolveDetailed(type: any, context: TypeResolutionContext = {}): ResolvedType {
        if (!type) {
            return this.createResolvedType('Object', false, false, false, false);
        }

        // Handle AST object types
        if (typeof type === 'object' && type !== null) {
            return this.resolveObjectType(type, context);
        }

        // Handle string types
        if (typeof type === 'string') {
            return this.resolveStringType(type, context);
        }

        // Fallback
        return this.createResolvedType(String(type), false, false, false, false);
    }

    /**
     * Context-specific resolution methods
     */
    static resolveForEntity(type: any): string {
        return this.resolve(type, { targetContext: 'entity', convertToDtos: false });
    }

    static resolveForDto(type: any): string {
        const resolved = this.resolveDetailed(type, { targetContext: 'dto', convertToDtos: false });
        // For DTOs, we want the actual type, not converted to DTO
        return resolved.javaType;
    }

    static resolveForWebApi(type: any): string {
        return this.resolve(type, { targetContext: 'webapi', convertToDtos: true });
    }

    static resolveForService(type: any): string {
        return this.resolve(type, { targetContext: 'service', convertToDtos: false });
    }

    static resolveForRepository(type: any): string {
        return this.resolve(type, { targetContext: 'repository', convertToDtos: false });
    }

    /**
     * Collection type handling
     */
    static isCollectionType(type: any): boolean {
        const resolved = this.resolveDetailed(type);
        return resolved.isCollection;
    }

    static getElementType(type: any): string | undefined {
        const resolved = this.resolveDetailed(type);
        return resolved.elementType;
    }

    /**
     * Type classification methods
     */
    static isPrimitiveType(typeName: string): boolean {
        const primitives = [
            'string', 'integer', 'long', 'boolean', 'localdatetime',
            'float', 'double', 'bigdecimal', 'byte', 'short', 'char'
        ];
        return primitives.includes(typeName.toLowerCase());
    }

    static isEntityType(typeName: string | any): boolean {
        if (typeof typeName === 'object' && typeName !== null) {
            if ('name' in typeName) {
                typeName = typeName.name;
            } else if ('typeName' in typeName) {
                typeName = typeName.typeName;
            } else {
                return false;
            }
        }

        if (typeof typeName !== 'string') {
            return false;
        }

        return !this.isPrimitiveType(typeName) &&
            !this.isBuiltinType(typeName) &&
            !this.isEnumType(typeName) &&
            typeName.charAt(0) === typeName.charAt(0).toUpperCase();
    }

    static isEnumType(typeName: string): boolean {
        return typeof typeName === 'string' && typeName.match(/^[A-Z][a-zA-Z]*Type$/) !== null;
    }

    static isBuiltinType(typeName: string): boolean {
        const builtins = [
            'userdto', 'aggregatestate', 'unitofwork', 'aggregate.aggregatestate',
            'object', 'void', 'exception'
        ];
        return builtins.includes(typeName.toLowerCase());
    }

    static isDtoCandidate(typeName: string): boolean {
        return this.isEntityType(typeName) && !typeName.endsWith('Dto');
    }

    /**
     * Private helper methods
     */
    private static resolveObjectType(type: any, context: TypeResolutionContext): ResolvedType {
        // Handle AST ListType
        if (type.$type === 'ListType' && type.elementType) {
            const elementType = this.extractElementTypeName(type.elementType);
            const resolvedElementType = this.applyContextConversion(elementType, context);
            return this.createResolvedType(
                `List<${resolvedElementType}>`,
                true,
                this.isPrimitiveType(elementType),
                this.isEntityType(elementType),
                this.isBuiltinType(elementType),
                elementType
            );
        }

        // Handle AST SetType
        if (type.$type === 'SetType' && type.elementType) {
            const elementType = this.extractElementTypeName(type.elementType);
            const resolvedElementType = this.applyContextConversion(elementType, context);
            return this.createResolvedType(
                `Set<${resolvedElementType}>`,
                true,
                this.isPrimitiveType(elementType),
                this.isEntityType(elementType),
                this.isBuiltinType(elementType),
                elementType
            );
        }

        // Handle CollectionType (generic collection type)
        if (type.$type === 'CollectionType' && type.elementType) {
            const elementType = this.extractElementTypeName(type.elementType);
            const resolvedElementType = this.applyContextConversion(elementType, context);
            // Determine collection type from the AST node text
            const sourceText = type.$cstNode?.text || '';
            const collectionTypeName = sourceText.startsWith('List<') ? 'List' : 'Set';
            return this.createResolvedType(
                `${collectionTypeName}<${resolvedElementType}>`,
                true,
                this.isPrimitiveType(elementType),
                this.isEntityType(elementType),
                this.isBuiltinType(elementType),
                elementType
            );
        }

        // Handle AggregateStateType
        if (type.$type === 'AggregateStateType') {
            return this.createResolvedType('AggregateState', false, false, false, true);
        }

        // Handle EntityType references
        if (type.$type === 'EntityType' && type.type) {
            const typeName = type.type.ref?.name || type.type.$refText;
            if (typeName) {
                const resolvedType = this.applyContextConversion(typeName, context);
                return this.createResolvedType(
                    resolvedType,
                    false,
                    this.isPrimitiveType(typeName),
                    this.isEntityType(typeName),
                    this.isBuiltinType(typeName)
                );
            }
        }

        // Handle PrimitiveType
        if (type.$type === 'PrimitiveType' && type.name) {
            const mappedType = this.mapPrimitiveType(type.name);
            return this.createResolvedType(mappedType, false, true, false, false);
        }

        // Handle ReturnType (method return types)
        if (type.$type === 'ReturnType') {
            return this.createResolvedType('void', false, true, false, true);
        }

        // Handle objects with name or typeName properties
        if ('name' in type) {
            return this.resolveStringType(type.name, context);
        } else if ('typeName' in type) {
            return this.resolveStringType(type.typeName, context);
        }

        // Fallback for unknown object types
        return this.createResolvedType('Object', false, false, false, false);
    }

    private static resolveStringType(typeName: string, context: TypeResolutionContext): ResolvedType {
        const normalizedName = typeName.toLowerCase();

        // Handle collection types in string format
        if (normalizedName.startsWith('set<') || normalizedName.startsWith('list<')) {
            const isSet = normalizedName.startsWith('set<');
            const elementType = this.extractElementTypeFromString(typeName);
            const resolvedElementType = this.applyContextConversion(elementType, context);
            const collectionType = isSet ? 'Set' : 'List';

            return this.createResolvedType(
                `${collectionType}<${resolvedElementType}>`,
                true,
                this.isPrimitiveType(elementType),
                this.isEntityType(elementType),
                this.isBuiltinType(elementType),
                elementType
            );
        }

        // Handle primitive and entity types
        const mappedType = this.mapPrimitiveType(typeName);
        const resolvedType = this.applyContextConversion(mappedType, context);

        return this.createResolvedType(
            resolvedType,
            false,
            this.isPrimitiveType(mappedType),
            this.isEntityType(mappedType),
            this.isBuiltinType(mappedType)
        );
    }

    private static applyContextConversion(typeName: string, context: TypeResolutionContext): string {
        // Apply DTO conversion for WebAPI context
        if (context.convertToDtos && context.targetContext === 'webapi') {
            if (this.shouldConvertToDto(typeName)) {
                return `${typeName}Dto`;
            }
        }

        return typeName;
    }

    private static shouldConvertToDto(typeName: string): boolean {
        // Don't convert primitives and built-in Java types
        const doNotConvert = [
            'String', 'Integer', 'Boolean', 'Double', 'Float', 'Long',
            'LocalDateTime', 'Object', 'void', 'BigDecimal',
            'UnitOfWork', 'AggregateState'
        ];

        if (doNotConvert.includes(typeName)) {
            return false;
        }

        // Don't convert types that already end with 'Dto'
        if (typeName.endsWith('Dto')) {
            return false;
        }

        // Convert entity types to DTOs
        return this.isEntityType(typeName);
    }

    private static extractElementTypeName(elementType: any): string {
        if (typeof elementType === 'string') {
            return elementType;
        }

        if (elementType && typeof elementType === 'object') {
            // Handle EntityType
            if (elementType.$type === 'EntityType' && elementType.type) {
                if (elementType.type.ref && elementType.type.ref.name) {
                    return elementType.type.ref.name;
                }
                if (elementType.type.$refText) {
                    return elementType.type.$refText;
                }
                if (elementType.type.name) {
                    return elementType.type.name;
                }
            }

            // Handle PrimitiveType
            if (elementType.$type === 'PrimitiveType') {
                return elementType.name || elementType.typeName || 'UnknownPrimitive';
            }

            // Handle ID references
            if (elementType.$refText) {
                return elementType.$refText;
            }
            if (elementType.ref && elementType.ref.name) {
                return elementType.ref.name;
            }
            if (elementType.name) {
                return elementType.name;
            }
        }

        return 'Object';
    }

    private static extractElementTypeFromString(typeName: string): string {
        const match = typeName.match(/^(Set|List)<(.+)>$/i);
        return match ? match[2].trim() : 'Object';
    }

    private static mapPrimitiveType(typeName: string): string {
        const lowerTypeName = typeName.toLowerCase();
        const typeMap: { [key: string]: string } = {
            'string': 'String',
            'integer': 'Integer',
            'long': 'Long',
            'boolean': 'Boolean',
            'localdatetime': 'LocalDateTime',
            'bigdecimal': 'BigDecimal',
            'float': 'Float',
            'double': 'Double',
            'byte': 'Byte',
            'short': 'Short',
            'char': 'Character',
            'userdto': 'UserDto',
            'aggregatestate': 'AggregateState',
            'unitofwork': 'UnitOfWork',
            'void': 'void',
            'object': 'Object'
        };

        return typeMap[lowerTypeName] || typeName; // Return original case-sensitive name for entities
    }

    private static createResolvedType(
        javaType: string,
        isCollection: boolean,
        isPrimitive: boolean,
        isEntity: boolean,
        isBuiltin: boolean,
        elementType?: string
    ): ResolvedType {
        return {
            javaType,
            isCollection,
            elementType,
            isPrimitive,
            isEntity,
            isBuiltin,
            isDtoCandidate: this.isDtoCandidate(javaType)
        };
    }

    /**
     * Utility method to safely get type name from various type objects
     */
    static safeGetTypeName(fieldType: any): string | null {
        if (!fieldType || typeof fieldType !== 'object') {
            return null;
        }

        if ('name' in fieldType) {
            return fieldType.name;
        }

        if ('typeName' in fieldType) {
            return fieldType.typeName;
        }

        return null;
    }

    /**
     * Legacy compatibility methods (to ease migration)
     */
    static resolveJavaType(fieldType: any): string {
        return this.resolve(fieldType);
    }

    static resolveParameterType(type: any): string {
        return this.resolveForWebApi(type);
    }
}
