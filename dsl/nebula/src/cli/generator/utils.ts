import { Entity, Property, Type as AstType, PrimitiveType, CollectionType, EntityType, AggregateStateType } from "../../language/generated/ast.js";
import * as fs from "node:fs/promises";
import * as path from "node:path";

/**
 * Helper function to capitalize the first letter of a string
 */
export function capitalize(str: string): string {
    if (!str) return str;
    return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * Type guards for AST types
 */
export function isPrimitiveType(type: AstType): type is PrimitiveType {
    return type.$type === "PrimitiveType";
}

export function isCollectionType(type: AstType): type is CollectionType {
    return type.$type === "CollectionType";
}

export function isEntityType(type: AstType): type is EntityType {
    return type.$type === "EntityType";
}

export function isAggregateStateType(type: AstType): type is AggregateStateType {
    return type.$type === "AggregateStateType";
}

/**
 * Maps a Nebula primitive type to its Java equivalent
 */
export function mapPrimitiveType(typeName: string): string {
    const primitiveTypes: { [key: string]: string } = {
        "Integer": "Integer",
        "Long": "Long",
        "String": "String",
        "Boolean": "boolean",
        "LocalDateTime": "LocalDateTime"
    };

    if (typeName in primitiveTypes) {
        return primitiveTypes[typeName];
    }

    console.warn(`Warning: Unrecognized type '${typeName}'. Defaulting to 'Object'.`);
    return "Object";
}

/**
 * Resolves a Nebula type to its Java equivalent
 */
export function resolveJavaType(type: AstType): string {
    if (isPrimitiveType(type)) {
        return mapPrimitiveType(type.typeName);
    } else if (isCollectionType(type)) {
        if (isEntityType(type.elementType)) {
            return `Set<${type.elementType.type.ref?.name || 'Object'}>`;
        } else if (isPrimitiveType(type.elementType)) {
            return `Set<${mapPrimitiveType(type.elementType.typeName)}>`;
        }
        return 'Set<Object>';
    } else if (isEntityType(type)) {
        return type.type?.ref?.name || 'Object';
    } else if (isAggregateStateType(type)) {
        return 'Aggregate.AggregateState';
    }

    console.warn(`Warning: Unrecognized type '${type}'. Defaulting to 'Object'.`);
    return "Object";
}

/**
 * Finds the property with the specified name in the entity
 */
export function findPropertyByName(entity: Entity, name: string): Property | undefined {
    return entity.properties.find(p => p.name === name);
}

/**
 * Logger utility functions
 */
export const logger = {
    info: (message: string) => console.log(`\x1b[32m${message}\x1b[0m`),
    warn: (message: string) => console.warn(`\x1b[33m${message}\x1b[0m`),
    error: (message: string) => console.error(`\x1b[31m${message}\x1b[0m`)
};

/**
 * Returns a file path that does not yet exist by appending an `_n` suffix
 * before the file extension whenever necessary.
 * Example: `User.java` -> `User_1.java`, `User_2.java`, ...
 */
export async function getUniqueFilePath(originalPath: string): Promise<string> {
    let candidate = originalPath;
    let counter = 1;
    const { dir, name, ext } = path.parse(originalPath);

    while (true) {
        try {
            await fs.access(candidate);
            // File exists – build a new candidate with suffix
            candidate = path.join(dir, `${name}_${counter}${ext}`);
            counter++;
        } catch {
            // File does not exist – return it
            return candidate;
        }
    }
} 