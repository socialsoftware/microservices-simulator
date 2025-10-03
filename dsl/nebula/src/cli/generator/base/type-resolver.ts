export interface PropertyType {
    name?: string;
    typeName?: string;
    isCollection?: boolean;
    elementType?: string;
    isPrimitive?: boolean;
    isEntity?: boolean;
    isBuiltin?: boolean;
}

export interface ResolvedType {
    javaType: string;
    isCollection: boolean;
    elementType?: string;
    isPrimitive: boolean;
    isEntity: boolean;
    isBuiltin: boolean;
}

export class TypeResolver {
    static resolveJavaType(fieldType: any): string {
        const resolved = this.resolveType(fieldType);
        return resolved.javaType;
    }

    static resolveType(fieldType: any): ResolvedType {
        if (typeof fieldType === 'object' && fieldType !== null) {
            // Handle AST ListType
            if (fieldType.$type === 'ListType' && fieldType.elementType) {
                const elementTypeName = this.extractElementTypeName(fieldType.elementType);
                return {
                    javaType: `List<${elementTypeName}>`,
                    isCollection: true,
                    elementType: elementTypeName,
                    isPrimitive: false,
                    isEntity: this.isEntityType(elementTypeName),
                    isBuiltin: false
                };
            }
            // Handle AST SetType
            if (fieldType.$type === 'SetType' && fieldType.elementType) {
                const elementTypeName = this.extractElementTypeName(fieldType.elementType);
                return {
                    javaType: `Set<${elementTypeName}>`,
                    isCollection: true,
                    elementType: elementTypeName,
                    isPrimitive: false,
                    isEntity: this.isEntityType(elementTypeName),
                    isBuiltin: false
                };
            }
            // Handle CollectionType (generic collection type)
            if (fieldType.$type === 'CollectionType' && fieldType.elementType) {
                const elementTypeName = this.extractElementTypeName(fieldType.elementType);
                return {
                    javaType: `Set<${elementTypeName}>`,
                    isCollection: true,
                    elementType: elementTypeName,
                    isPrimitive: false,
                    isEntity: this.isEntityType(elementTypeName),
                    isBuiltin: false
                };
            }
            // Handle AggregateStateType
            if (fieldType.$type === 'AggregateStateType') {
                return {
                    javaType: 'AggregateState',
                    isCollection: false,
                    elementType: undefined,
                    isPrimitive: false,
                    isEntity: false,
                    isBuiltin: true
                };
            }
            // Handle EntityType references (can be Entity or EnumDefinition)
            if (fieldType.$type === 'EntityType' && fieldType.type) {
                // Try to get the name from the resolved reference
                if (fieldType.type.ref && fieldType.type.ref.name) {
                    const typeName = fieldType.type.ref.name;
                    return this.resolveTypeFromName(typeName);
                }
                // Fall back to the reference text if the reference is not resolved yet
                if (fieldType.type.$refText) {
                    return this.resolveTypeFromName(fieldType.type.$refText);
                }
            }
            // Handle PrimitiveType
            if (fieldType.$type === 'PrimitiveType' && fieldType.name) {
                return this.resolveTypeFromName(fieldType.name);
            }
            // Handle ReturnType (method return types)
            if (fieldType.$type === 'ReturnType') {
                return {
                    javaType: 'void',
                    isCollection: false,
                    isPrimitive: true,
                    isEntity: false,
                    isBuiltin: true
                };
            }
            if ('name' in fieldType) {
                const typeName = fieldType.name;
                return this.resolveTypeFromName(typeName);
            } else if ('typeName' in fieldType) {
                const typeName = fieldType.typeName;
                return this.resolveTypeFromName(typeName);
            } else {
                return {
                    javaType: 'Object',
                    isCollection: false,
                    isPrimitive: false,
                    isEntity: false,
                    isBuiltin: false
                };
            }
        }

        if (typeof fieldType === 'string') {
            return this.resolveTypeFromName(fieldType);
        }

        return {
            javaType: String(fieldType),
            isCollection: false,
            isPrimitive: false,
            isEntity: false,
            isBuiltin: false
        };
    }

    private static resolveTypeFromName(typeName: string): ResolvedType {
        const normalizedName = typeName.toLowerCase();

        // More precise check for collections - must start with set< or list<
        const isCollection = normalizedName.startsWith('set<') || normalizedName.startsWith('list<') ||
            normalizedName === 'set' || normalizedName === 'list';

        let elementType: string | undefined;
        let javaType: string;

        if (isCollection) {
            if (normalizedName.startsWith('set<')) {
                elementType = this.extractElementType(typeName, 'Set<', '>');
                javaType = `Set<${elementType}>`;
            } else if (normalizedName.startsWith('list<')) {
                elementType = this.extractElementType(typeName, 'List<', '>');
                javaType = `List<${elementType}>`;
            } else {
                javaType = 'Collection';
            }
        } else {
            javaType = this.mapPrimitiveType(typeName);
        }

        return {
            javaType,
            isCollection,
            elementType,
            isPrimitive: this.isPrimitiveType(normalizedName),
            isEntity: this.isEntityType(normalizedName),
            isBuiltin: this.isBuiltinType(normalizedName)
        };
    }

    private static extractElementType(typeName: string, prefix: string, suffix: string): string {
        const start = typeName.indexOf(prefix) + prefix.length;
        const end = typeName.lastIndexOf(suffix);
        if (start > prefix.length - 1 && end > start) {
            return typeName.substring(start, end).trim();
        }
        return 'Object';
    }

    private static extractElementTypeName(elementType: any): string {
        if (typeof elementType === 'string') {
            return elementType;
        }
        if (elementType && typeof elementType === 'object') {
            // Handle EntityType (can be Entity or EnumDefinition)
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
            if (elementType.$type === 'PrimitiveType' && elementType.name) {
                return elementType.name;
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

    private static mapPrimitiveType(typeName: string): string {
        const lowerTypeName = typeName.toLowerCase();
        switch (lowerTypeName) {
            case 'string':
                return 'String';
            case 'integer':
                return 'Integer';
            case 'long':
                return 'Long';
            case 'boolean':
                return 'Boolean';
            case 'localdatetime':
                return 'LocalDateTime';
            case 'userdto':
                return 'UserDto';
            case 'aggregatestate':
                return 'AggregateState';
            case 'unitofwork':
                return 'UnitOfWork';
            default:
                // For entity types, return the original case-sensitive name
                return typeName;
        }
    }

    private static isPrimitiveType(typeName: string): boolean {
        const primitives = ['string', 'integer', 'long', 'boolean', 'localdatetime'];
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
        return !this.isPrimitiveType(typeName) &&
            !this.isBuiltinType(typeName) &&
            !this.isEnumType(typeName) &&
            typeName.charAt(0) === typeName.charAt(0).toUpperCase();
    }

    static isEnumType(typeName: string): boolean {
        return typeof typeName === 'string' && typeName.match(/^[A-Z][a-zA-Z]*Type$/) !== null;
    }

    private static isBuiltinType(typeName: string): boolean {
        const builtins = ['userdto', 'aggregatestate', 'unitofwork', 'aggregate.aggregatestate'];
        return builtins.includes(typeName.toLowerCase());
    }

    static isCollectionType(fieldType: any): boolean {
        const resolved = this.resolveType(fieldType);
        return resolved.isCollection;
    }

    static getElementType(fieldType: any): string | undefined {
        const resolved = this.resolveType(fieldType);
        return resolved.elementType;
    }

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
}
