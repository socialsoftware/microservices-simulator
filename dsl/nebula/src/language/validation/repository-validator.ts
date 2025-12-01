import type { ValidationAcceptor } from "langium";
import type { Aggregate, Entity, RepositoryMethod } from "../generated/ast.js";
import { getEntities } from "../../cli/utils/aggregate-helpers.js";
import { QueryMethodParser } from "../../cli/utils/query-method-parser.js";
import { JpqlParser } from "./utils/jpql-parser.js";
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

            const paramPattern = /:(\w+)/g;
            const queryParams: string[] = [];
            let paramMatch;
            while ((paramMatch = paramPattern.exec(method.query)) !== null) {
                const paramName = paramMatch[1];
                if (!queryParams.includes(paramName)) {
                    queryParams.push(paramName);
                }
            }
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
        const parser = new JpqlParser();
        const parseResult = parser.parse(query);

        if (!parseResult.isValid) {
            for (const error of parseResult.errors) {
                accept("error", error.message, {
                    node: method,
                    property: "query",
                });
            }
            return;
        }

        if (!parseResult.ast) {
            accept("error", "Failed to parse query", {
                node: method,
                property: "query",
            });
            return;
        }

        const { ast, aliases } = parseResult;

        this.validateJpqlAst(ast, aliases, entities, method, accept);
    }

    private validateJpqlAst(
        ast: any,
        aliases: Map<string, string>,
        entities: Entity[],
        method: RepositoryMethod,
        accept: ValidationAcceptor
    ): void {
        const fromEntity = entities.find((e: any) => e.name === ast.from.entity);
        if (!fromEntity) {
            const suggestions = entities
                .map((e: any) => e.name)
                .filter((name: string) => {
                    const nameLower = name.toLowerCase();
                    const entityLower = ast.from.entity.toLowerCase();
                    return nameLower === entityLower ||
                        nameLower.includes(entityLower) ||
                        entityLower.includes(nameLower);
                })
                .slice(0, 3);
            const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
            accept("error", `Entity '${ast.from.entity}' not found in aggregate.${suggestionText}`, {
                node: method,
                property: "query",
            });
        }

        for (const item of ast.select.items) {
            this.validatePropertyPath(item.path, aliases, entities, method, accept);
        }

        if (ast.where) {
            this.validateCondition(ast.where.condition, aliases, entities, method, accept);
        }
    }

    private validateCondition(
        condition: any,
        aliases: Map<string, string>,
        entities: Entity[],
        method: RepositoryMethod,
        accept: ValidationAcceptor
    ): void {
        if (condition.type === 'grouped' && condition.left) {
            this.validateCondition(condition.left, aliases, entities, method, accept);
        } else if (condition.type === 'and' || condition.type === 'or') {
            if (condition.left) {
                this.validateCondition(condition.left, aliases, entities, method, accept);
            }
            if (condition.right) {
                this.validateCondition(condition.right, aliases, entities, method, accept);
            }
        } else if (condition.comparison) {
            this.validateComparison(condition.comparison, aliases, entities, method, accept);
        }
    }

    private validateComparison(
        comparison: any,
        aliases: Map<string, string>,
        entities: Entity[],
        method: RepositoryMethod,
        accept: ValidationAcceptor
    ): void {
        this.validatePropertyPath(comparison.left, aliases, entities, method, accept);

        if (comparison.right && comparison.right.type === 'property') {
            this.validatePropertyPath(comparison.right, aliases, entities, method, accept);
        }

        if (comparison.right && comparison.right.type === 'subquery') {
            const subquery = comparison.right;
            this.validateJpqlAst(subquery.ast, subquery.aliases, entities, method, accept);
        }
    }

    private validatePropertyPath(
        path: any,
        aliases: Map<string, string>,
        entities: Entity[],
        method: RepositoryMethod,
        accept: ValidationAcceptor
    ): void {
        if (!path.properties || path.properties.length === 0) {
            return;
        }

        const firstProperty = path.properties[0];
        const remainingProperties = path.properties.slice(1);

        let entityName: string | undefined;
        let entity: Entity | undefined;

        if (path.alias) {
            entityName = aliases.get(path.alias);
            if (!entityName) {
                const definedAliases = Array.from(aliases.keys());
                const suggestions = definedAliases.filter(alias => {
                    const aliasLower = alias.toLowerCase();
                    const undefinedLower = path.alias.toLowerCase();
                    return aliasLower.includes(undefinedLower) || undefinedLower.includes(aliasLower);
                }).slice(0, 3);
                const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                accept("error", `Undefined alias '${path.alias}' in query.${suggestionText} Available aliases: ${definedAliases.length > 0 ? definedAliases.join(', ') : 'none'}`, {
                    node: method,
                    property: "query",
                });
                return;
            }
            entity = entities.find((e: any) => e.name === entityName);
        } else {
            entity = entities.find((e: any) => e.name === firstProperty);
            if (entity) {
                entityName = firstProperty;
                if (remainingProperties.length === 0) {
                    accept("error", `Expected property after entity '${firstProperty}'`, {
                        node: method,
                        property: "query",
                    });
                    return;
                }
            } else {
                entityName = Array.from(aliases.values())[0];
                entity = entities.find((e: any) => e.name === entityName);
            }
        }

        if (!entity) {
            return;
        }

        let currentEntity = entity;
        const propertiesToValidate = entityName === firstProperty ? remainingProperties : path.properties;

        for (let i = 0; i < propertiesToValidate.length; i++) {
            const propName = propertiesToValidate[i];

            const property = PropertyValidatorUtils.findPropertyInEntity(propName, currentEntity, entities);
            if (!property) {
                const suggestions = PropertyValidatorUtils.suggestPropertyNames(propName, currentEntity, entities);
                const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
                accept("error", `Property '${propName}' not found on entity '${currentEntity.name}'.${suggestionText}`, {
                    node: method,
                    property: "query",
                });
                return;
            }

            if (i < propertiesToValidate.length - 1) {
                const propType = (property as any).type;
                if (propType?.$type === 'EntityType' && propType.type) {
                    const relatedEntityName = propType.type.ref?.name || propType.type.$refText;
                    if (relatedEntityName) {
                        currentEntity = entities.find((e: any) => e.name === relatedEntityName)!;
                        if (!currentEntity) {
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    accept("error", `Property '${propName}' on entity '${currentEntity.name}' is not an entity reference and cannot have nested properties`, {
                        node: method,
                        property: "query",
                    });
                    return;
                }
            }
        }
    }
}

