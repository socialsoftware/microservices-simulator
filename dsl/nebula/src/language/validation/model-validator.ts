import type { ValidationAcceptor } from "langium";
import type { Model, Aggregate } from "../generated/ast.js";
import { ErrorMessageProvider } from "../error-messages.js";
import { getEntities, getMethods } from "../../cli/utils/aggregate-helpers.js";
import { NamingValidator } from "./naming-validator.js";

export class ModelValidator {
    constructor(private readonly namingValidator: NamingValidator) { }

    checkModel(model: Model, accept: ValidationAcceptor): void {
        const aggregateNames = new Set<string>();
        for (const aggregate of model.aggregates) {
            if (!aggregate.name) {
                accept("error", "Aggregate name is required", {
                    node: aggregate,
                    property: "name",
                });
                continue;
            }
            if (aggregateNames.has(aggregate.name.toLowerCase())) {
                const errorMsg = ErrorMessageProvider.getMessage('DUPLICATE_AGGREGATE_NAME', { name: aggregate.name });
                accept("error", errorMsg.message, {
                    node: aggregate,
                    property: "name",
                });
            } else {
                aggregateNames.add(aggregate.name.toLowerCase());
            }
        }

        if (model.aggregates.length === 0) {
            const warningMsg = ErrorMessageProvider.getMessage('EMPTY_MODEL');
            accept("warning", warningMsg.message, {
                node: model,
            });
        }
    }

    checkAggregate(aggregate: Aggregate, accept: ValidationAcceptor): void {
        this.namingValidator.validateName(aggregate.name, "aggregate", aggregate, accept);

        const entities = getEntities(aggregate);
        const methods = getMethods(aggregate);

        const entityNames = new Set<string>();
        for (const entity of entities) {
            if (entityNames.has(entity.name.toLowerCase())) {
                accept("error", `Duplicate entity name: ${entity.name}`, {
                    node: entity,
                    property: "name",
                });
            } else {
                entityNames.add(entity.name.toLowerCase());
            }
        }

        const rootEntities = entities.filter((e: any) => e.isRoot);
        if (rootEntities.length === 0) {
            accept("warning", "Aggregate should have at least one root entity", {
                node: aggregate,
            });
        } else if (rootEntities.length > 1) {
            accept("error", "Aggregate can only have one root entity", {
                node: aggregate,
            });
        }

        const methodNames = new Set<string>();
        for (const method of methods) {
            if (methodNames.has(method.name.toLowerCase())) {
                accept("error", `Duplicate method name: ${method.name}`, {
                    node: method,
                    property: "name",
                });
            } else {
                methodNames.add(method.name.toLowerCase());
            }
        }
    }
}

