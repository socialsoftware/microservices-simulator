import type { ValidationAcceptor, ValidationChecks } from "langium";
import type { NebulaAstType, Model, Aggregate, Entity, Property, Method, Invariant, BusinessRule } from "./generated/ast.js";
import type { NebulaServices } from "./nebula-module.js";
import { ErrorMessageProvider } from "./error-messages.js";

export function registerValidationChecks(services: NebulaServices) {
  const registry = services.validation.ValidationRegistry;
  const validator = services.validation.NebulaValidator;
  const checks: ValidationChecks<NebulaAstType> = {
    Model: validator.checkModel,
    Aggregate: validator.checkAggregate,
    Entity: validator.checkEntity,
    Property: validator.checkProperty,
    Method: validator.checkMethod,
    Invariant: validator.checkInvariant,
    BusinessRule: validator.checkBusinessRule,
  };
  registry.register(checks, validator);
}

export class NebulaValidator {
  private readonly reservedWords = new Set([
    'class', 'interface', 'enum', 'package', 'import', 'public', 'private', 'protected',
    'static', 'final', 'abstract', 'extends', 'implements', 'new', 'this', 'super',
    'if', 'else', 'for', 'while', 'do', 'switch', 'case', 'default', 'break', 'continue',
    'return', 'try', 'catch', 'finally', 'throw', 'throws', 'void', 'int', 'long',
    'float', 'double', 'boolean', 'char', 'byte', 'short', 'String', 'Object',
    'List', 'Set', 'Map', 'Collection', 'ArrayList', 'HashMap', 'HashSet'
  ]);

  private readonly javaNamingPattern = /^[a-zA-Z_$][a-zA-Z0-9_$]*$/;

  checkModel(model: Model, accept: ValidationAcceptor): void {
    const aggregateNames = new Set<string>();
    for (const aggregate of model.aggregates) {
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
    this.validateName(aggregate.name, "aggregate", aggregate, accept);

    const entityNames = new Set<string>();
    for (const entity of aggregate.entities) {
      if (entityNames.has(entity.name.toLowerCase())) {
        accept("error", `Duplicate entity name: ${entity.name}`, {
          node: entity,
          property: "name",
        });
      } else {
        entityNames.add(entity.name.toLowerCase());
      }
    }

    // Check for root entity
    const rootEntities = aggregate.entities.filter((e: any) => e.isRoot);
    if (rootEntities.length === 0) {
      accept("warning", "Aggregate should have at least one root entity", {
        node: aggregate,
      });
    } else if (rootEntities.length > 1) {
      accept("error", "Aggregate can only have one root entity", {
        node: aggregate,
      });
    }

    // Check for duplicate method names
    const methodNames = new Set<string>();
    for (const method of aggregate.methods || []) {
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

  checkEntity(entity: Entity, accept: ValidationAcceptor): void {
    this.validateName(entity.name, "entity", entity, accept);

    const propertyNames = new Set<string>();
    for (const property of entity.properties) {
      if (!property.name) {
        accept("error", "Property name cannot be empty", {
          node: property,
          property: "name",
        });
        continue;
      }
      if (propertyNames.has(property.name.toLowerCase())) {
        accept("error", `Duplicate property name: ${property.name}`, {
          node: property,
          property: "name",
        });
      } else {
        propertyNames.add(property.name.toLowerCase());
      }
    }

    // Check for required properties in root entities
    if (entity.isRoot) {
      const hasId = entity.properties.some(p => p.name && (p.name.toLowerCase() === 'id' || p.isKey));
      if (!hasId) {
        accept("warning", "Root entity should have an 'id' property or a key property", {
          node: entity,
        });
      }
    }
  }

  checkProperty(property: Property, accept: ValidationAcceptor): void {
    // Validate property name
    this.validateName(property.name, "property", property, accept);

    // Check for valid type
    if (!property.type) {
      accept("error", "Property must have a type", {
        node: property,
        property: "type",
      });
    }

    // Check for collection properties with valid element types
    if (property.type && typeof property.type === 'object' && 'elementType' in property.type) {
      if (!property.type.elementType) {
        accept("error", "Collection property must specify element type", {
          node: property,
          property: "type",
        });
      }
    }
  }

  checkMethod(method: Method, accept: ValidationAcceptor): void {
    // Validate method name
    this.validateName(method.name, "method", method, accept);

    // Check for duplicate parameter names
    const paramNames = new Set<string>();
    for (const param of method.parameters || []) {
      if (param.name && paramNames.has(param.name.toLowerCase())) {
        accept("error", `Duplicate parameter name: ${param.name}`, {
          node: param,
          property: "name",
        });
      } else if (param.name) {
        paramNames.add(param.name.toLowerCase());
      }
    }

    // Validate return type
    if (method.returnType && typeof method.returnType === 'string') {
      if (!this.javaNamingPattern.test(method.returnType)) {
        accept("error", `Invalid return type: ${method.returnType}`, {
          node: method,
          property: "returnType",
        });
      }
    }
  }

  checkInvariant(invariant: Invariant, accept: ValidationAcceptor): void {
    // Validate invariant name
    this.validateName(invariant.name, "invariant", invariant, accept);

    // Check for empty conditions
    if (!invariant.conditions || invariant.conditions.length === 0) {
      accept("error", "Invariant must have at least one condition", {
        node: invariant,
        property: "conditions",
      });
    }

    // Check for valid condition syntax (basic check)
    for (const condition of invariant.conditions || []) {
      if (condition && typeof condition === 'object' && 'expression' in condition) {
        const expr = (condition as any).expression;
        if (expr && typeof expr === 'string' && !expr.includes(';')) {
          accept("warning", "Invariant condition should end with semicolon", {
            node: invariant,
            property: "conditions",
          });
        }
      }
    }
  }

  checkBusinessRule(businessRule: BusinessRule, accept: ValidationAcceptor): void {
    // Validate business rule name
    this.validateName(businessRule.name, "business rule", businessRule, accept);

    // Check for empty conditions
    if (!businessRule.conditions || businessRule.conditions.length === 0) {
      accept("error", "Business rule must have at least one condition", {
        node: businessRule,
        property: "conditions",
      });
    }

    // Check for exception message
    if (!businessRule.exception || businessRule.exception.trim() === '') {
      accept("warning", "Business rule should have an exception message", {
        node: businessRule,
        property: "exception",
      });
    }
  }

  private validateName(name: string, type: string, node: any, accept: ValidationAcceptor): void {
    // Check for empty name
    if (!name || name.trim() === '') {
      accept("error", `${type} name cannot be empty`, {
        node: node,
        property: "name",
      });
      return;
    }

    // Check for reserved words
    if (this.reservedWords.has(name.toLowerCase())) {
      accept("error", `'${name}' is a reserved word and cannot be used as ${type} name`, {
        node: node,
        property: "name",
      });
    }

    // Check Java naming conventions
    if (!this.javaNamingPattern.test(name)) {
      accept("error", `Invalid ${type} name '${name}'. Must start with letter or underscore and contain only letters, digits, underscores, and dollar signs`, {
        node: node,
        property: "name",
      });
    }

    // Check for proper casing
    if (type === 'entity' || type === 'aggregate') {
      if (name[0] !== name[0].toUpperCase()) {
        accept("warning", `${type} name should start with uppercase letter`, {
          node: node,
          property: "name",
        });
      }
    } else if (type === 'property' || type === 'method') {
      if (name[0] !== name[0].toLowerCase()) {
        accept("warning", `${type} name should start with lowercase letter`, {
          node: node,
          property: "name",
        });
      }
    }
  }
}
