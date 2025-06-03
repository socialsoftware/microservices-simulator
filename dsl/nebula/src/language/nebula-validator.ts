import type { ValidationAcceptor, ValidationChecks } from "langium";
import type { NebulaAstType, Model, Aggregate } from "./generated/ast.js";
import type { NebulaServices } from "./nebula-module.js";

/**
 * Register custom validation checks.
 */
export function registerValidationChecks(services: NebulaServices) {
  const registry = services.validation.ValidationRegistry;
  const validator = services.validation.NebulaValidator;
  const checks: ValidationChecks<NebulaAstType> = {
    Model: validator.checkAgregateNames,
    Aggregate: validator.checkEntityNames,
  };
  registry.register(checks, validator);
}

/**
 * Implementation of custom validations.
 */
export class NebulaValidator {
  checkAgregateNames(model: Model, accept: ValidationAcceptor): void {
    const aggregates = model.aggregates;
    const previousNames = new Set<string>();
    for (const aggregate of aggregates) {
      if (previousNames.has(aggregate.name.toLowerCase())) {
        accept("error", "Duplicate name for aggregate", {
          node: aggregate,
          property: "name",
        });
      } else {
        previousNames.add(aggregate.name.toLowerCase());
      }
    }
  }
  checkEntityNames(aggregate: Aggregate, accept: ValidationAcceptor): void {
    const entities = aggregate.entities;
    const previousNames = new Set<string>();
    for (const entity of entities) {
      if (previousNames.has(entity.name.toLowerCase())) {
        accept("error", "Duplicate name for entity", {
          node: entity,
          property: "name",
        });
      } else {
        previousNames.add(entity.name.toLowerCase());
      }
    }
  }
}
