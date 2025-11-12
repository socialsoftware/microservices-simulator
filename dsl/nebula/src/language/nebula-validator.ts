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

    this.validateSharedDtoFields(model, accept);
    this.validateRootEntityDtoCoverage(model, accept);
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

  private validateSharedDtoFields(model: Model, accept: ValidationAcceptor): void {
    const sharedBlocks: any[] = (model as any).sharedDtos || [];
    if (!sharedBlocks.length) return;

    for (const block of sharedBlocks) {
      for (const dto of (block.dtos || [])) {
        const dtoName: string = dto.name;
        if (!dtoName) continue;
        const aggregateName = dtoName.endsWith('Dto') ? dtoName.slice(0, -3) : dtoName;
        const aggregate = model.aggregates.find(a => a.name === aggregateName);
        if (!aggregate) continue;

        const entities = getEntities(aggregate);
        const root = entities.find((e: any) => e.isRoot);
        if (!root) continue;
        const rootProps: string[] = (root.properties || []).map((p: any) => p.name);

        for (const field of dto.fields || []) {
          const f: any = field;
          const fieldName: string = f.name;
          if (!fieldName) continue;

          const mappedEntityName: string | undefined = f.sourceEntity?.ref?.name || f.sourceEntity?.$refText;
          if (mappedEntityName) {
            const exists = entities.some((e: any) => e.name === mappedEntityName);
            if (!exists) {
              accept("error", `DTO '${dtoName}' field '${fieldName}' maps to entity '${mappedEntityName}', which does not exist in aggregate '${aggregateName}'.`, {
                node: field as any,
                property: "sourceEntity",
              });
            }
          }

          const typeText: string = (f.type?.$cstNode?.text || '').trim();
          const isCollection = typeText.startsWith('List<') || typeText.startsWith('Set<');
          const isSingleDto = !isCollection && typeText.endsWith('Dto');
          if (isSingleDto || isCollection) {
            if (!rootProps.includes(fieldName)) {
              const accessor = `get${fieldName[0].toUpperCase()}${fieldName.slice(1)}()`;
              accept("error", `DTO '${dtoName}' field '${fieldName}' requires root property '${fieldName}' in aggregate '${aggregateName}' to generate accessor ${accessor}.`, {
                node: field as any,
                property: "name",
              });
            }
          }
        }
      }
    }
  }

  private validateRootEntityDtoCoverage(model: Model, accept: ValidationAcceptor): void {
    const allDtos: any[] = this.collectAllSharedDtoDefinitions(model);
    const standardDtoFields = new Set<string>(['aggregateId', 'version', 'state']);

    for (const aggregate of model.aggregates) {
      const aggregateName = aggregate.name;
      const entities = getEntities(aggregate);
      const root = entities.find((e: any) => e.isRoot);
      if (!root) continue;

      const rootAny = root as any;
      const explicitDtoRef = rootAny.dtoType;
      const dtoName =
        explicitDtoRef?.ref?.name ||
        explicitDtoRef?.$refText ||
        `${aggregateName}Dto`;

      if (!dtoName) {
        continue;
      }

      const dto = allDtos.find((d: any) => d.name === dtoName);
      if (!dto) {
        accept("error", `Root entity '${root.name}' requires DTO '${dtoName}' but it was not found. Create the DTO in a shared-dtos block or reference an existing one using 'uses dto'.`, {
          node: root as any,
          property: "name",
        });
        continue;
      }

      const dtoFieldNames = new Set<string>((dto.fields || []).map((f: any) => f.name));
      for (const standardField of standardDtoFields) {
        dtoFieldNames.add(standardField);
      }

      const fieldMappings = new Map<string, string>();
      const mappings: any[] = rootAny.dtoMapping?.fieldMappings || [];
      for (const mapping of mappings) {
        if (mapping?.entityField && mapping?.dtoField) {
          fieldMappings.set(mapping.entityField, mapping.dtoField);
        }
      }

      for (const prop of (root.properties || [])) {
        const propName: string = prop.name;
        if (!propName) continue;

        if (propName.toLowerCase() === 'id') {
          continue;
        }

        if (this.isEntityReferenceProperty(prop)) {
          continue;
        }

        const mappedDtoField = fieldMappings.get(propName) || propName;
        if (!dtoFieldNames.has(mappedDtoField)) {
          accept("error", `DTO '${dtoName}' is missing field '${mappedDtoField}' required to represent root property '${propName}' in aggregate '${aggregateName}'.`, {
            node: prop as any,
            property: "name",
          });
        }
      }
    }
  }

  private collectAllSharedDtoDefinitions(model: Model): any[] {
    const collected: any[] = [];
    const seen = new Set<string>();

    const collectFromModel = (target?: Model) => {
      if (!target) return;
      const sharedBlocks: any[] = (target as any).sharedDtos || [];
      for (const block of sharedBlocks) {
        for (const dto of (block.dtos || [])) {
          if (dto?.name && !seen.has(dto.name)) {
            seen.add(dto.name);
            collected.push(dto);
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

  private isEntityReferenceProperty(prop: Property): boolean {
    const type: any = (prop as any).type;
    return this.isEntityReferenceType(type);
  }

  private isEntityReferenceType(type: any): boolean {
    if (!type || typeof type !== 'object') {
      return false;
    }

    const typeKind: string | undefined = type.$type;
    if (!typeKind) {
      return false;
    }

    if (typeKind === 'EntityType') {
      return true;
    }

    if (typeKind === 'CollectionType') {
      const elementType = (type as any).elementType;
      if (typeof elementType === 'object') {
        return this.isEntityReferenceType(elementType);
      }
      return false;
    }

    if (typeKind === 'OptionalType') {
      const elementType = (type as any).elementType;
      if (typeof elementType === 'object') {
        return this.isEntityReferenceType(elementType);
      }
      return false;
    }

    if (typeKind === 'AggregateStateType') {
      return false;
    }

    return false;
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
    }

    const entityAny = entity as any;
    if (entityAny.dtoType) {
      this.validateDtoImport(entity, accept);
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

  private validateDtoImport(entity: Entity, accept: ValidationAcceptor): void {
    const model = entity.$container?.$container;
    if (!model) return;

    const modelAny = model as any;
    const hasSharedDtosImport = modelAny.imports?.some((imp: any) =>
      imp.sharedDtos === true
    );

    if (!hasSharedDtosImport) {
      const entityAny = entity as any;
      const dtoName = entityAny.dtoType?.ref?.name || entityAny.dtoType?.$refText || 'unknown';
      accept("error", `Entity '${entity.name}' uses DTO '${dtoName}' but 'shared-dtos' is not imported. Add 'import shared-dtos;' at the top of the file.`, {
        node: entity,
        property: "dtoType",
      });
    }
  }

  private validateEntityDtoMapping(entity: Entity, fieldMappings: any[], accept: ValidationAcceptor): void {
    const entityFields = entity.properties.map(p => p.name);

    const entityAny = entity as any;
    const dtoType = entityAny.dtoType;
    const dtoDefinition = dtoType?.ref;

    for (const mapping of fieldMappings) {
      if (!entityFields.includes(mapping.entityField)) {
        accept("error", `Entity field '${mapping.entityField}' does not exist in entity '${entity.name}'. Available fields: ${entityFields.join(', ')}`, {
          node: mapping,
          property: "entityField",
        });
      }

      if (dtoDefinition && dtoDefinition.fields) {
        const explicitDtoFields = dtoDefinition.fields.map((f: any) => f.name);
        const standardFields = ['aggregateId', 'version', 'state'];
        const allDtoFields = [...standardFields, ...explicitDtoFields];

        if (!allDtoFields.includes(mapping.dtoField)) {
          accept("error", `DTO field '${mapping.dtoField}' does not exist in DTO '${dtoDefinition.name}'. Available fields: ${allDtoFields.join(', ')}`, {
            node: mapping,
            property: "dtoField",
          });
        }
      } else if (dtoType && !dtoDefinition) {
        accept("error", `Referenced DTO '${dtoType.$refText || 'unknown'}' not found`, {
          node: entity,
          property: "dtoType",
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
      }
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
}
