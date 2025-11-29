import type { ValidationAcceptor } from "langium";
import type { Aggregate, Entity, RepositoryMethod } from "../generated/ast.js";
import { getEntities } from "../../cli/utils/aggregate-helpers.js";
import { QueryMethodParser } from "../../cli/utils/query-method-parser.js";
import { JpqlValidatorUtils } from "./utils/jpql-validator-utils.js";
import { PropertyValidatorUtils } from "./utils/property-validator-utils.js";

export class RepositoryValidator {
    checkRepositoryMethod(method: RepositoryMethod, accept: ValidationAcceptor): void {
        const aggregate = method.$container?.$container as Aggregate | undefined;
        if (!aggregate) {
            return;
        }

        const entities = getEntities(aggregate);
        const rootEntity = entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            return;
        }

        if (method.query) {
            if (method.query.trim() === '') {
                accept("error", "Query string cannot be empty", {
                    node: method,
                    property: "query",
                });
                return;
            }

            const queryParams = JpqlValidatorUtils.extractQueryParameters(method.query);
            const methodParamNames = (method.parameters || []).map((p: any) => p.name);

            for (const queryParam of queryParams) {
                if (!methodParamNames.includes(queryParam)) {
                    accept("error", `Query parameter ':${queryParam}' does not match any method parameter. Available parameters: ${methodParamNames.length > 0 ? methodParamNames.join(', ') : 'none'}`, {
                        node: method,
                        property: "query",
                    });
                }
            }

            for (const methodParam of methodParamNames) {
                if (!queryParams.includes(methodParam)) {
                    accept("warning", `Method parameter '${methodParam}' is not used in the query`, {
                        node: method,
                        property: "parameters",
                    });
                }
            }

            this.validateJpqlQuery(method.query, entities, rootEntity, accept, method);
            return;
        }

        const parsed = QueryMethodParser.parse(method.name);

        if (!parsed.isValid) {
            for (const error of parsed.errors) {
                accept("error", error, {
                    node: method,
                    property: "name",
                });
            }
            return;
        }

        const allEntities = entities;
        const propertyValidation = QueryMethodParser.validateProperties(parsed, rootEntity, allEntities);
        if (!propertyValidation.isValid) {
            for (const error of propertyValidation.errors) {
                accept("error", error, {
                    node: method,
                    property: "name",
                });
            }
        }

        const paramCount = method.parameters?.length || 0;
        const paramValidation = QueryMethodParser.validateParameterCount(parsed, paramCount);
        if (!paramValidation.isValid) {
            for (const error of paramValidation.errors) {
                accept("error", error, {
                    node: method,
                    property: "parameters",
                });
            }
        }

        const returnTypeText = this.getReturnTypeText(method.returnType);
        if (returnTypeText) {
            const returnTypeValidation = QueryMethodParser.validateReturnType(parsed.prefix, returnTypeText);
            if (!returnTypeValidation.isValid) {
                for (const error of returnTypeValidation.errors) {
                    accept("error", error, {
                        node: method,
                        property: "returnType",
                    });
                }
            }
        }
    }

    private getReturnTypeText(returnType: any): string | null {
        if (!returnType) {
            return null;
        }

        if (returnType.$cstNode && returnType.$cstNode.text) {
            return returnType.$cstNode.text.trim();
        }

        if (returnType.type) {
            return this.getReturnTypeText(returnType.type);
        }

        if (returnType.name) {
            return returnType.name;
        }

        return null;
    }

    private validateJpqlQuery(
        query: string,
        entities: Entity[],
        rootEntity: Entity,
        accept: ValidationAcceptor,
        method: RepositoryMethod
    ): void {
        let cleanQuery = query.trim();
        if ((cleanQuery.startsWith('"') && cleanQuery.endsWith('"')) ||
            (cleanQuery.startsWith("'") && cleanQuery.endsWith("'"))) {
            cleanQuery = cleanQuery.slice(1, -1);
        }
        const queryUpper = cleanQuery.toUpperCase().trim();

        if (!queryUpper.match(/\b(SELECT|FROM)\b/i)) {
            accept("error", "Query must contain SELECT and FROM clauses", {
                node: method,
                property: "query",
            });
            return;
        }

        const syntaxErrors = JpqlValidatorUtils.validateJpqlSyntax(cleanQuery);
        for (const error of syntaxErrors) {
            accept("error", error.message, {
                node: method,
                property: "query",
            });
        }

        if (syntaxErrors.length > 0) {
            return;
        }

        const fromPattern = /\bFROM\s+(?:AS\s+)?(\w+)(?:\s+(?:AS\s+)?(\w+))?/gi;
        const fromMatches = Array.from(cleanQuery.matchAll(fromPattern));
        const entityAliases = new Map<string, string>(); // alias -> entityName

        if (fromMatches.length > 0) {
            for (const fromMatch of fromMatches) {
                const entityName = fromMatch[1];
                const alias = fromMatch[2] || entityName.charAt(0).toLowerCase();

                const entity = entities.find((e: any) => e.name === entityName);
                if (!entity) {
                    const suggestions = entities
                        .map((e: any) => e.name)
                        .filter((name: string) => {
                            const nameLower = name.toLowerCase();
                            const entityLower = entityName.toLowerCase();
                            return nameLower === entityLower ||
                                nameLower.includes(entityLower) ||
                                entityLower.includes(nameLower);
                        })
                        .slice(0, 3);
                    const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                    accept("error", `Entity '${entityName}' not found in aggregate.${suggestionText}`, {
                        node: method,
                        property: "query",
                    });
                } else {
                    entityAliases.set(alias, entityName);
                }
            }
        } else {
            const simpleFromMatch = cleanQuery.match(/\bFROM\s+(\w+)/i);
            if (simpleFromMatch) {
                const entityName = simpleFromMatch[1];
                const entity = entities.find((e: any) => e.name === entityName);
                if (!entity) {
                    const suggestions = entities
                        .map((e: any) => e.name)
                        .filter((name: string) => {
                            const nameLower = name.toLowerCase();
                            const entityLower = entityName.toLowerCase();
                            return nameLower === entityLower ||
                                nameLower.includes(entityLower) ||
                                entityLower.includes(nameLower);
                        })
                        .slice(0, 3);
                    const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                    accept("error", `Entity '${entityName}' not found in aggregate.${suggestionText}`, {
                        node: method,
                        property: "query",
                    });
                }
            }
        }

        const propertyPattern = /(\w+)\.(\w+)/g;
        let propertyMatch;
        const validatedProperties = new Set<string>();
        const validatedAliases = new Set<string>();

        while ((propertyMatch = propertyPattern.exec(cleanQuery)) !== null) {
            const aliasOrEntity = propertyMatch[1];
            const propertyName = propertyMatch[2];

            if (cleanQuery.substring(propertyMatch.index - 1, propertyMatch.index) === ':') {
                continue;
            }

            if (entityAliases.has(aliasOrEntity)) {
                const entityName = entityAliases.get(aliasOrEntity)!;
                const entity = entities.find((e: any) => e.name === entityName);

                if (entity) {
                    const property = PropertyValidatorUtils.findPropertyInEntity(propertyName, entity, entities);
                    if (!property) {
                        const propertyKey = `${aliasOrEntity}.${propertyName}`;
                        if (!validatedProperties.has(propertyKey)) {
                            validatedProperties.add(propertyKey);
                            const suggestions = PropertyValidatorUtils.suggestPropertyNames(propertyName, entity, entities);
                            const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                            accept("error", `Property '${propertyName}' not found on entity '${entityName}'.${suggestionText}`, {
                                node: method,
                                property: "query",
                            });
                        }
                    }
                }
            } else {
                const entity = entities.find((e: any) => e.name === aliasOrEntity);
                if (entity) {
                    const property = PropertyValidatorUtils.findPropertyInEntity(propertyName, entity, entities);
                    if (!property) {
                        const propertyKey = `${aliasOrEntity}.${propertyName}`;
                        if (!validatedProperties.has(propertyKey)) {
                            validatedProperties.add(propertyKey);
                            const suggestions = PropertyValidatorUtils.suggestPropertyNames(propertyName, entity, entities);
                            const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                            accept("error", `Property '${propertyName}' not found on entity '${aliasOrEntity}'.${suggestionText}`, {
                                node: method,
                                property: "query",
                            });
                        }
                    }
                } else {
                    const aliasKey = `alias_${aliasOrEntity}`;
                    if (!validatedAliases.has(aliasKey)) {
                        validatedAliases.add(aliasKey);
                        const definedAliases = Array.from(entityAliases.keys());
                        const suggestions = definedAliases.filter(alias => {
                            const aliasLower = alias.toLowerCase();
                            const undefinedLower = aliasOrEntity.toLowerCase();
                            return aliasLower.includes(undefinedLower) || undefinedLower.includes(aliasLower);
                        }).slice(0, 3);
                        const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                        accept("error", `Undefined alias '${aliasOrEntity}' in query.${suggestionText} Available aliases: ${definedAliases.length > 0 ? definedAliases.join(', ') : 'none'}`, {
                            node: method,
                            property: "query",
                        });
                    }
                }
            }
        }
    }
}

