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

        const isCollection = normalizedName.includes('set') || normalizedName.includes('list');

        let elementType: string | undefined;
        let javaType: string;

        if (isCollection) {
            if (normalizedName.includes('set<')) {
                elementType = this.extractElementType(typeName, 'Set<', '>');
                javaType = `Set<${elementType}>`;
            } else if (normalizedName.includes('list<')) {
                elementType = this.extractElementType(typeName, 'List<', '>');
                javaType = `List<${elementType}>`;
            } else {
                javaType = 'Collection';
            }
        } else {
            javaType = this.mapPrimitiveType(normalizedName);
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

    private static mapPrimitiveType(typeName: string): string {
        switch (typeName) {
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
                return typeName;
        }
    }

    private static isPrimitiveType(typeName: string): boolean {
        const primitives = ['string', 'integer', 'long', 'boolean', 'localdatetime'];
        return primitives.includes(typeName);
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
            typeName.charAt(0) === typeName.charAt(0).toUpperCase();
    }

    private static isBuiltinType(typeName: string): boolean {
        const builtins = ['userdto', 'aggregatestate', 'unitofwork'];
        return builtins.includes(typeName);
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
