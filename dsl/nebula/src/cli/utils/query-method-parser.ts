import { Entity, Property } from "../../language/generated/ast.js";

export interface ParsedQueryMethod {
    prefix: 'find' | 'exists' | 'count' | 'delete' | 'remove' | null;
    properties: string[];
    keywords: string[];
    sortFields: string[];
    limit?: number;
    isValid: boolean;
    errors: string[];
}

export class QueryMethodParser {
    private static readonly QUERY_PREFIXES = [
        'find', 'findAll', 'findFirst', 'findTop',
        'exists', 'count', 'delete', 'remove'
    ];

    private static readonly KEYWORDS: Record<string, { params: number; requiresProperty: boolean }> = {
        'And': { params: 0, requiresProperty: true },
        'Or': { params: 0, requiresProperty: true },
        'Not': { params: 0, requiresProperty: true },
        'Is': { params: 1, requiresProperty: false },
        'Equals': { params: 1, requiresProperty: false },
        'Between': { params: 2, requiresProperty: false },
        'LessThan': { params: 1, requiresProperty: false },
        'LessThanEqual': { params: 1, requiresProperty: false },
        'GreaterThan': { params: 1, requiresProperty: false },
        'GreaterThanEqual': { params: 1, requiresProperty: false },
        'Before': { params: 1, requiresProperty: false },
        'After': { params: 1, requiresProperty: false },
        'Like': { params: 1, requiresProperty: false },
        'NotLike': { params: 1, requiresProperty: false },
        'StartingWith': { params: 1, requiresProperty: false },
        'EndingWith': { params: 1, requiresProperty: false },
        'Containing': { params: 1, requiresProperty: false },
        'IgnoreCase': { params: 0, requiresProperty: false },
        'IsNull': { params: 0, requiresProperty: false },
        'IsNotNull': { params: 0, requiresProperty: false },
        'Null': { params: 0, requiresProperty: false },
        'NotNull': { params: 0, requiresProperty: false },
        'In': { params: 1, requiresProperty: false },
        'NotIn': { params: 1, requiresProperty: false },
        'True': { params: 0, requiresProperty: false },
        'False': { params: 0, requiresProperty: false }
    };

    static parse(methodName: string): ParsedQueryMethod {
        const result: ParsedQueryMethod = {
            prefix: null,
            properties: [],
            keywords: [],
            sortFields: [],
            isValid: true,
            errors: []
        };

        if (!methodName?.trim()) {
            result.isValid = false;
            result.errors.push('Method name cannot be empty');
            return result;
        }

        const prefixMatch = methodName.match(/^(find(?:All|First|Top)?|exists|count|delete|remove)/i);
        if (!prefixMatch) {
            result.isValid = false;
            result.errors.push(`Invalid query method prefix. Expected one of: ${this.QUERY_PREFIXES.join(', ')}`);
            return result;
        }

        const prefix = prefixMatch[1].toLowerCase();
        if (prefix.startsWith('find')) {
            result.prefix = 'find';
            const limitMatch = methodName.match(/^(?:findFirst|findTop)(\d+)/i);
            if (limitMatch) {
                result.limit = parseInt(limitMatch[1], 10);
            }
        } else if (prefix === 'exists') {
            result.prefix = 'exists';
        } else if (prefix === 'count') {
            result.prefix = 'count';
        } else if (prefix === 'delete' || prefix === 'remove') {
            result.prefix = prefix === 'delete' ? 'delete' : 'remove';
        }

        const byIndex = methodName.toLowerCase().indexOf('by');
        if (byIndex === -1) {
            if (prefix.startsWith('find') && !methodName.toLowerCase().includes('by')) {
                return result;
            }
            result.isValid = false;
            result.errors.push('Query method must contain "By" after the prefix');
            return result;
        }

        const afterBy = methodName.substring(byIndex + 2);
        const orderByMatch = afterBy.match(/OrderBy(.+)$/i);
        let queryPart = afterBy;
        if (orderByMatch) {
            queryPart = afterBy.substring(0, orderByMatch.index);
            result.sortFields = this.parseSortFields(orderByMatch[1]);
        }

        const parsed = this.parseQueryPart(queryPart);
        result.properties = parsed.properties;
        result.keywords = parsed.keywords;

        const keywordErrors = this.validateKeywordSequence(result.keywords, result.properties);
        result.errors.push(...keywordErrors);
        if (keywordErrors.length > 0) {
            result.isValid = false;
        }

        return result;
    }

    private static parseQueryPart(queryPart: string): { properties: string[]; keywords: string[] } {
        const properties: string[] = [];
        const keywords: string[] = [];
        let remaining = queryPart;
        let currentProperty = '';

        while (remaining.length > 0) {
            let matched = false;
            const sortedKeywords = Object.keys(this.KEYWORDS).sort((a, b) => b.length - a.length);

            for (const keyword of sortedKeywords) {
                if (remaining.startsWith(keyword)) {
                    if (currentProperty) {
                        properties.push(currentProperty);
                        currentProperty = '';
                    }
                    keywords.push(keyword);
                    remaining = remaining.substring(keyword.length);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                currentProperty += remaining[0];
                remaining = remaining.substring(1);
            }
        }

        if (currentProperty) {
            properties.push(currentProperty);
        }

        return { properties, keywords };
    }

    private static parseSortFields(orderByPart: string): string[] {
        const fields: string[] = [];
        let current = '';

        for (let i = 0; i < orderByPart.length; i++) {
            const char = orderByPart[i];
            if (char >= 'A' && char <= 'Z' && current.length > 0) {
                fields.push(current);
                current = char;
            } else {
                current += char;
            }
        }

        if (current) {
            const cleaned = current.replace(/(Asc|Desc)$/i, '');
            if (cleaned) {
                fields.push(cleaned);
            }
        }

        return fields;
    }

    private static validateKeywordSequence(keywords: string[], properties: string[]): string[] {
        const errors: string[] = [];

        if (keywords.length === 0) {
            return errors;
        }

        const andOrKeywords = keywords.filter(k => k === 'And' || k === 'Or');
        if (andOrKeywords.length > 0) {
            if (properties.length < 2) {
                errors.push(`Keywords 'And'/'Or' require at least two properties, got ${properties.length}`);
            } else if (properties.length < andOrKeywords.length + 1) {
                errors.push(`Not enough properties for 'And'/'Or' keywords. Need ${andOrKeywords.length + 1} properties, got ${properties.length}`);
            }
        }

        for (const keyword of keywords) {
            const keywordInfo = this.KEYWORDS[keyword] as { params: number; requiresProperty: boolean } | undefined;
            if (!keywordInfo) {
                errors.push(`Unknown keyword: ${keyword}`);
                continue;
            }
            if (keywordInfo.requiresProperty && properties.length === 0) {
                errors.push(`Keyword '${keyword}' must follow a property name`);
            }
        }

        return errors;
    }

    static validateProperties(parsed: ParsedQueryMethod, entity: Entity, allEntities?: Entity[]): { isValid: boolean; errors: string[] } {
        const errors: string[] = [];

        for (const propertyName of parsed.properties) {
            const property = this.findProperty(propertyName, entity, allEntities);
            if (!property) {
                const suggestions = this.suggestPropertyNames(propertyName, entity, allEntities);
                if (suggestions.length > 0) {
                    errors.push(`Property '${propertyName}' not found on entity '${entity.name}'. Did you mean: ${suggestions.join(', ')}?`);
                } else {
                    errors.push(`Property '${propertyName}' not found on entity '${entity.name}'`);
                }
            }
        }

        return {
            isValid: errors.length === 0,
            errors
        };
    }

    private static findProperty(propertyName: string, entity: Entity, allEntities?: Entity[]): Property | null {
        const camelCaseName = propertyName.charAt(0).toLowerCase() + propertyName.slice(1);

        const directMatch = entity.properties?.find(p =>
            p.name.toLowerCase() === propertyName.toLowerCase() ||
            p.name.toLowerCase() === camelCaseName.toLowerCase()
        );
        if (directMatch) {
            return directMatch;
        }

        const camelCaseMatch = entity.properties?.find(p => p.name === camelCaseName);
        if (camelCaseMatch) {
            return camelCaseMatch;
        }

        if (entity.properties && allEntities) {
            for (const prop of entity.properties) {
                const relationPascalCase = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                if (propertyName.startsWith(relationPascalCase)) {
                    const remaining = propertyName.substring(relationPascalCase.length);
                    if (remaining) {
                        const propType = (prop as any).type;
                        if (propType?.$type === 'EntityType' && propType.type) {
                            const relatedEntityName = propType.type.ref?.name || propType.type.$refText;
                            if (relatedEntityName) {
                                const relatedEntity = allEntities.find(e => e.name === relatedEntityName);
                                if (relatedEntity) {
                                    const nestedProperty = this.findProperty(remaining, relatedEntity, allEntities);
                                    if (nestedProperty) {
                                        return prop;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static suggestPropertyNames(propertyName: string, entity: Entity, allEntities?: Entity[]): string[] {
        const suggestions: string[] = [];
        const propertyNameLower = propertyName.toLowerCase();

        if (entity.properties) {
            for (const prop of entity.properties) {
                const propNameLower = prop.name.toLowerCase();
                if (propNameLower.includes(propertyNameLower) || propertyNameLower.includes(propNameLower)) {
                    suggestions.push(prop.name);
                }
            }
        }

        if (entity.properties && allEntities) {
            for (const prop of entity.properties) {
                const propType = (prop as any).type;
                if (propType?.$type === 'EntityType' && propType.type) {
                    const relatedEntityName = propType.type.ref?.name || propType.type.$refText;
                    if (relatedEntityName) {
                        const relatedEntity = allEntities.find(e => e.name === relatedEntityName);
                        if (relatedEntity?.properties) {
                            const relationPascalCase = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
                            for (const nestedProp of relatedEntity.properties) {
                                const nestedPascalCase = nestedProp.name.charAt(0).toUpperCase() + nestedProp.name.slice(1);
                                const nestedPropertyName = relationPascalCase + nestedPascalCase;
                                if (nestedPropertyName.toLowerCase().includes(propertyNameLower)) {
                                    suggestions.push(nestedPropertyName);
                                }
                            }
                        }
                    }
                }
            }
        }

        return suggestions.slice(0, 3);
    }

    static validateParameterCount(parsed: ParsedQueryMethod, actualParamCount: number): { isValid: boolean; errors: string[] } {
        const errors: string[] = [];
        let expectedCount = 0;

        if (parsed.properties.length === 0 && parsed.keywords.length === 0) {
            if (actualParamCount !== 0) {
                errors.push(`Parameter count mismatch: expected 0 for method without 'By' clause, got ${actualParamCount}`);
            }
            return {
                isValid: errors.length === 0,
                errors
            };
        }

        for (let i = 0; i < parsed.properties.length; i++) {
            const keywordAfter = i < parsed.keywords.length ? parsed.keywords[i] : undefined;
            const keywordInfo = keywordAfter ? this.KEYWORDS[keywordAfter] as { params: number; requiresProperty: boolean } | undefined : undefined;
            if (!keywordInfo || (keywordInfo.params > 0) || keywordAfter === 'And' || keywordAfter === 'Or') {
                expectedCount++;
            }
        }

        for (const keyword of parsed.keywords) {
            const keywordInfo = this.KEYWORDS[keyword] as { params: number; requiresProperty: boolean } | undefined;
            if (keywordInfo && keywordInfo.params > 0 && keyword === 'Between') {
                expectedCount += 1;
            }
        }

        if (expectedCount !== actualParamCount) {
            errors.push(`Parameter count mismatch: expected ${expectedCount}, got ${actualParamCount}`);
        }

        return {
            isValid: errors.length === 0,
            errors
        };
    }

    static validateReturnType(prefix: string | null, returnType: string): { isValid: boolean; errors: string[] } {
        const errors: string[] = [];

        if (!prefix) {
            return { isValid: true, errors };
        }

        const returnTypeLower = returnType.toLowerCase();

        switch (prefix) {
            case 'find':
                break;
            case 'exists':
                if (returnTypeLower !== 'boolean') {
                    errors.push(`Return type for 'exists' methods should be 'Boolean', got '${returnType}'`);
                }
                break;
            case 'count':
                if (!returnTypeLower.includes('long') && !returnTypeLower.includes('integer') && !returnTypeLower.includes('int')) {
                    errors.push(`Return type for 'count' methods should be 'Long' or 'Integer', got '${returnType}'`);
                }
                break;
            case 'delete':
            case 'remove':
                if (returnTypeLower !== 'void' &&
                    !returnTypeLower.includes('long') &&
                    !returnTypeLower.includes('integer') &&
                    !returnTypeLower.includes('int')) {
                    errors.push(`Return type for '${prefix}' methods should be 'void', 'Long', or 'Integer', got '${returnType}'`);
                }
                break;
        }

        return {
            isValid: errors.length === 0,
            errors
        };
    }
}
