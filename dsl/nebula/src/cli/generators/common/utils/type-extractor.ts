


import { PRIMITIVE_TYPES } from './type-constants.js';



export class TypeExtractor {


    static extractEnumType(type: string): string | null {
        if (!type) return null;


        const typeName = type.replace(/List<|Set<|Optional<|>/g, '').trim();


        if (!typeName) return null;


        if (PRIMITIVE_TYPES.includes(typeName as any)) return null;


        if (typeName.endsWith('Dto')) return null;


        if (typeName.includes('<')) return null;


        if (typeName.charAt(0) !== typeName.charAt(0).toUpperCase()) return null;

        return typeName;
    }



    static extractEnumTypes(type: string, enumSet: Set<string>): void {
        const enumType = this.extractEnumType(type);
        if (enumType) {
            enumSet.add(enumType);
        }
    }



    static extractEnumsFromCode(
        javaCode: string,
        excludedEnums: string[] = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState', 'LocalDateTime']
    ): Set<string> {
        const foundEnums = new Set<string>();

        const enumPattern = /\b([A-Z][a-zA-Z]*(?:Type|Role|State))\b/g;
        let match;

        while ((match = enumPattern.exec(javaCode)) !== null) {
            const enumType = match[1];
            if (excludedEnums.includes(enumType)) continue;
            if (enumType.endsWith('Dto')) continue;
            if (enumType.includes('List') || enumType.includes('Set')) continue;
            foundEnums.add(enumType);
        }

        const enumeratedPattern = /@Enumerated\([^)]*\)\s+private\s+(?:final\s+)?(\w+)\s+/g;
        while ((match = enumeratedPattern.exec(javaCode)) !== null) {
            const enumType = match[1];
            if (!excludedEnums.includes(enumType) && !enumType.endsWith('Dto')) {
                foundEnums.add(enumType);
            }
        }

        return foundEnums;
    }



    static extractDtosFromCode(javaCode: string): Set<string> {
        const dtoPattern = /(\w+Dto)\b/g;
        const dtos = new Set<string>();
        let match;

        while ((match = dtoPattern.exec(javaCode)) !== null) {
            dtos.add(match[1]);
        }

        return dtos;
    }



    static extractCollectionElementType(type: string): string | null {
        if (!type) return null;


        const match = type.match(/(?:List|Set|Optional)<(.+)>/);
        return match ? match[1].trim() : null;
    }



    static isCollectionType(type: string): boolean {
        if (!type) return false;
        return type.startsWith('List<') || type.startsWith('Set<') || type.startsWith('Optional<');
    }



    static isDtoType(type: string): boolean {
        if (!type) return false;
        return type.endsWith('Dto');
    }



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

                const elementType = this.extractCollectionElementType(type);
                if (elementType) {
                    types.push(elementType);
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
