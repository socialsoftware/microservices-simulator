import type { ValidationAcceptor, ValidationChecks } from "langium";
import type { NebulaAstType, Model, Aggregate, Entity, Property, Method, Invariant, Import, RepositoryMethod } from "./generated/ast.js";
import type { NebulaServices } from "./nebula-module.js";
import { ErrorMessageProvider } from "./error-messages.js";
import { getEntities, getMethods } from "../cli/utils/aggregate-helpers.js";
import { QueryMethodParser } from "../cli/utils/query-method-parser.js";

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
    Import: validator.checkImport,
    RepositoryMethod: validator.checkRepositoryMethod,
  };
  registry.register(checks, validator);
}

export class NebulaValidator {
  constructor(private readonly services?: NebulaServices) { }

  private readonly reservedWords = new Set([
    'class', 'interface', 'enum', 'package', 'import', 'public', 'private', 'protected',
    'static', 'final', 'abstract', 'extends', 'implements', 'new', 'this', 'super',
    'if', 'else', 'for', 'while', 'do', 'switch', 'case', 'default', 'break', 'continue',
    'return', 'try', 'catch', 'finally', 'throw', 'throws', 'void', 'int', 'long',
    'float', 'double', 'boolean', 'char', 'byte', 'short', 'String', 'Object',
    'List', 'Set', 'Map', 'Collection', 'ArrayList', 'HashMap', 'HashSet'
  ]);

  private readonly javaNamingPattern = /^[a-zA-Z_$][a-zA-Z0-9_$]*$/;

  // ============================================================================
  // MODEL VALIDATION
  // ============================================================================

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
    this.validateName(aggregate.name, "aggregate", aggregate, accept);

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

  private collectAllDtoEnabledEntities(model: Model): Map<string, Entity> {
    const collected = new Map<string, Entity>();

    const collectFromModel = (target?: Model) => {
      if (!target) return;
      for (const aggregate of target.aggregates || []) {
        for (const entity of getEntities(aggregate)) {
          const generateDto = (entity as any)?.generateDto;
          if ((entity.isRoot || generateDto) && entity.name && !collected.has(entity.name)) {
            collected.set(entity.name, entity);
          }
        }
      }
    };

    collectFromModel(model);

    const documentsService: any = (this.services as any)?.shared?.workspace?.LangiumDocuments;
    const documentStream = documentsService?.all;

    const iterateDocuments = (documents: any) => {
      if (!documents) return;
      if (typeof documents.forEach === 'function') {
        documents.forEach((doc: any) => {
          const root = doc?.parseResult?.value;
          if (root && root.$type === 'Model') {
            collectFromModel(root as Model);
          }
        });
        return;
      }
      if (typeof documents[Symbol.iterator] === 'function') {
        for (const doc of documents as any) {
          const root = doc?.parseResult?.value;
          if (root && root.$type === 'Model') {
            collectFromModel(root as Model);
          }
        }
      }
    };

    iterateDocuments(documentStream);

    return collected;
  }

  private getDtoFieldsForEntity(entity: Entity): Set<string> {
    const fields = new Set<string>();
    if (entity.isRoot) {
      fields.add('aggregateId');
      fields.add('version');
      fields.add('state');
    }

    for (const prop of entity.properties || []) {
      if (!prop?.name) continue;
      if (prop.dtoExclude) continue;

      const propertyNames = [prop.name, ...(prop.names || [])];
      for (const propName of propertyNames) {
        if (!propName) continue;
        fields.add(propName);

        if (this.isEntityReferenceProperty(prop)) {
          fields.add(`${propName}AggregateId`);
        }
      }
    }
    return fields;
  }

  private isEntityReferenceProperty(property: Property): boolean {
    if (!property?.type) {
      return false;
    }
    return (property.type as any).$type === 'EntityType';
  }

  // ============================================================================
  // ENTITY VALIDATION
  // ============================================================================

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

    if (entity.isRoot) {
      const hasId = entity.properties.some(p => p.name && p.name.toLowerCase() === 'id');
      if (!hasId) {
        accept("warning", "Root entity should have an 'id' property", {
          node: entity,
        });
      }

      for (const property of entity.properties) {
        if (property.dtoExclude && property.name && property.name.toLowerCase() === 'id') {
          accept("error", "Root entity id property cannot be marked with 'dto-exclude'.", {
            node: property as any,
            property: "name",
          });
        }
      }
    }

    const entityAny = entity as any;
    if (entity.generateDto && entityAny.dtoMapping) {
      accept("error", "Entities marked with 'Dto' cannot declare a DTO mapping block.", {
        node: entityAny.dtoMapping,
      });
    }
    if (entityAny.dtoMapping?.fieldMappings) {
      this.validateEntityDtoMapping(entity, entityAny.dtoMapping.fieldMappings, accept);
    }

    if (!entity.isRoot && entity.invariants && entity.invariants.length > 0) {
      accept("error", "Only root entities can have invariants. Non-root entities should not define invariant blocks.", {
        node: entity,
        property: "invariants",
      });
    }
  }

  // ============================================================================
  // PROPERTY VALIDATION
  // ============================================================================

  checkProperty(property: Property, accept: ValidationAcceptor): void {
    this.validateName(property.name, "property", property, accept);

    if (!property.type) {
      accept("error", "Property must have a type", {
        node: property,
        property: "type",
      });
    }

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
    this.validateName(method.name, "method", method, accept);

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
    this.validateName(invariant.name, "invariant", invariant, accept);

    if (!invariant.conditions || invariant.conditions.length === 0) {
      accept("error", "Invariant must have at least one condition", {
        node: invariant,
        property: "conditions",
      });
    }

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


  // ============================================================================
  // HELPER METHODS & UTILITIES
  // ============================================================================

  private validateName(name: string, type: string, node: any, accept: ValidationAcceptor): void {
    if (!name || name.trim() === '') {
      accept("error", `${type} name cannot be empty`, {
        node: node,
        property: "name",
      });
      return;
    }

    if (this.reservedWords.has(name.toLowerCase())) {
      accept("error", `'${name}' is a reserved word and cannot be used as ${type} name`, {
        node: node,
        property: "name",
      });
    }

    if (!this.javaNamingPattern.test(name)) {
      accept("error", `Invalid ${type} name '${name}'. Must start with letter or underscore and contain only letters, digits, underscores, and dollar signs`, {
        node: node,
        property: "name",
      });
    }

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

  checkImport(importNode: Import, accept: ValidationAcceptor): void {
  }


  // ============================================================================
  // DTO MAPPING VALIDATION
  // ============================================================================

  private validateEntityDtoMapping(entity: Entity, fieldMappings: any[], accept: ValidationAcceptor): void {
    const entityFields = entity.properties.map(p => p.name);

    const entityAny = entity as any;
    const dtoType = entityAny.dtoType;
    const model = entity.$container?.$container as Model | undefined;

    let targetDtoFields: Set<string> | undefined;
    if (model && dtoType) {
      const dtoName = dtoType;
      const dtoEnabledEntities = this.collectAllDtoEnabledEntities(model);
      if (dtoName.endsWith('Dto')) {
        const targetEntityName = dtoName.slice(0, -3);
        const targetEntity = dtoEnabledEntities.get(targetEntityName);
        if (!targetEntity) {
          accept("error", `DTO '${dtoName}' must correspond to entity '${targetEntityName}' that is marked as 'Root' or 'Dto', but none was found.`, {
            node: entity,
            property: "dtoType",
          });
        } else {
          targetDtoFields = this.getDtoFieldsForEntity(targetEntity);
        }
      } else {
        accept("error", `DTO reference '${dtoName}' must end with 'Dto' to correspond to a generated DTO.`, {
          node: entity,
          property: "dtoType",
        });
      }
    }

    for (const mapping of fieldMappings) {
      if (!entityFields.includes(mapping.entityField)) {
        accept("error", `Entity field '${mapping.entityField}' does not exist in entity '${entity.name}'. Available fields: ${entityFields.join(', ')}`, {
          node: mapping,
          property: "entityField",
        });
      }

      if (targetDtoFields && !targetDtoFields.has(mapping.dtoField)) {
        accept("error", `DTO field '${mapping.dtoField}' is not available on the target DTO. Available fields: ${Array.from(targetDtoFields).join(', ')}`, {
          node: mapping,
          property: "dtoField",
        });
      }

    }
  }

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

      const queryParams = this.extractQueryParameters(method.query);
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
    const cleanQuery = query.trim().replace(/^["']|["']$/g, '');
    const queryUpper = cleanQuery.toUpperCase().trim();

    if (!queryUpper.match(/\b(SELECT|FROM)\b/i)) {
      accept("error", "Query must contain SELECT and FROM clauses", {
        node: method,
        property: "query",
      });
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

    while ((propertyMatch = propertyPattern.exec(cleanQuery)) !== null) {
      const aliasOrEntity = propertyMatch[1];
      const propertyName = propertyMatch[2];

      if (cleanQuery.substring(propertyMatch.index - 1, propertyMatch.index) === ':') {
        continue;
      }

      const entityName = entityAliases.get(aliasOrEntity) || aliasOrEntity;
      const entity = entities.find((e: any) => e.name === entityName);

      if (entity) {
        const property = this.findPropertyInEntity(propertyName, entity, entities);
        if (!property) {
          const propertyKey = `${aliasOrEntity}.${propertyName}`;
          if (!validatedProperties.has(propertyKey)) {
            validatedProperties.add(propertyKey);
            const suggestions = this.suggestPropertyNames(propertyName, entity, entities);
            const suggestionText = suggestions.length > 0 ? ` Did you mean: ${suggestions.join(', ')}?` : '';
            accept("error", `Property '${propertyName}' not found on entity '${entityName}'.${suggestionText}`, {
              node: method,
              property: "query",
            });
          }
        }
      } else if (!entityAliases.has(aliasOrEntity) && !entities.find((e: any) => e.name === aliasOrEntity)) {
        const entityKey = `entity_${aliasOrEntity}`;
        if (!validatedProperties.has(entityKey)) {
          validatedProperties.add(entityKey);
        }
      }
    }
  }

  private findPropertyInEntity(propertyName: string, entity: Entity, allEntities: Entity[]): Property | null {
    const camelCaseName = propertyName.charAt(0).toLowerCase() + propertyName.slice(1);

    const directMatch = entity.properties?.find((p: any) =>
      p.name.toLowerCase() === propertyName.toLowerCase() ||
      p.name.toLowerCase() === camelCaseName.toLowerCase()
    );
    if (directMatch) {
      return directMatch;
    }

    const camelCaseMatch = entity.properties?.find((p: any) => p.name === camelCaseName);
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
                const relatedEntity = allEntities.find((e: any) => e.name === relatedEntityName);
                if (relatedEntity) {
                  const nestedProperty = this.findPropertyInEntity(remaining, relatedEntity, allEntities);
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

  private suggestPropertyNames(propertyName: string, entity: Entity, allEntities?: Entity[]): string[] {
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

    return suggestions.slice(0, 3);
  }

  private extractQueryParameters(query: string): string[] {
    const paramPattern = /:(\w+)/g;
    const params: string[] = [];
    let match;

    while ((match = paramPattern.exec(query)) !== null) {
      const paramName = match[1];
      if (!params.includes(paramName)) {
        params.push(paramName);
      }
    }

    return params;
  }
}
