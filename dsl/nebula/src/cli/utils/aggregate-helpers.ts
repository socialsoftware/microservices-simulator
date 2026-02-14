import { Aggregate, Entity, Method, Workflow, Repository, Events, References, WebAPIEndpoints, ServiceDefinition, Functionalities, isEntity, isMethod, isWorkflow, isRepository, isEvents, isReferences, isWebAPIEndpoints, isServiceDefinition, isFunctionalities, Model } from "../../language/generated/ast.js";

// ============================================================================
// CROSS-FILE MODEL REGISTRY
// ============================================================================
// This registry stores all parsed models to enable cross-file type inference.
// It must be initialized before code generation begins.

let allModelsRegistry: Model[] = [];

/**
 * Register all parsed models for cross-file type resolution.
 * Must be called before any code generation begins.
 */
export function registerAllModels(models: Model[]): void {
    allModelsRegistry = models;
}

/**
 * Get all registered models.
 */
export function getAllModels(): Model[] {
    return allModelsRegistry;
}

/**
 * Clear the models registry. Useful for testing.
 */
export function clearModelsRegistry(): void {
    allModelsRegistry = [];
}

// ============================================================================
// AGGREGATE ELEMENT ACCESSORS
// ============================================================================

export function getEntities(aggregate: Aggregate): Entity[] {
    if (!aggregate.aggregateElements || !Array.isArray(aggregate.aggregateElements)) {
        return [];
    }
    return aggregate.aggregateElements.filter((el): el is Entity => isEntity(el));
}

export function getMethods(aggregate: Aggregate): Method[] {
    return aggregate.aggregateElements.filter((el): el is Method => isMethod(el));
}

export function getWorkflows(aggregate: Aggregate): Workflow[] {
    return aggregate.aggregateElements.filter((el): el is Workflow => isWorkflow(el));
}

export function getRepository(aggregate: Aggregate): Repository | undefined {
    return aggregate.aggregateElements.find((el): el is Repository => isRepository(el));
}

export function getEvents(aggregate: Aggregate): Events | undefined {
    return aggregate.aggregateElements.find((el): el is Events => isEvents(el));
}

export function getReferences(aggregate: Aggregate): References | undefined {
    return aggregate.aggregateElements.find((el): el is References => isReferences(el));
}

export function getWebAPIEndpoints(aggregate: Aggregate): WebAPIEndpoints | undefined {
    return aggregate.aggregateElements.find((el): el is WebAPIEndpoints => isWebAPIEndpoints(el));
}

export function getServiceDefinition(aggregate: Aggregate): ServiceDefinition | undefined {
    return aggregate.aggregateElements.find((el): el is ServiceDefinition => isServiceDefinition(el));
}

export function getFunctionalities(aggregate: Aggregate): Functionalities | undefined {
    return aggregate.aggregateElements.find((el): el is Functionalities => isFunctionalities(el));
}

declare module "../../language/generated/ast.js" {
    interface Aggregate {
        entities: Entity[];
        methods: Method[];
        workflows: Workflow[];
        repository?: Repository;
        events?: Events;
        references?: References;
        webApiEndpoints?: WebAPIEndpoints;
        serviceDefinition?: ServiceDefinition;
        functionalities?: Functionalities;
    }
}

const initializedAggregates = new WeakSet<Aggregate>();

// Helper to ensure properties are initialized before access
function ensureInitialized(aggregate: Aggregate): void {
    if (!initializedAggregates.has(aggregate)) {
        initializeAggregateProperties(aggregate);
    }
}

export function initializeAggregateProperties(aggregate: Aggregate): void {
    if (initializedAggregates.has(aggregate)) {
        return;
    }

    // Ensure aggregateElements exists
    if (!aggregate.aggregateElements) {
        (aggregate as any).aggregateElements = [];
    }

    // Always set up getters, even if properties already exist
    try {
        Object.defineProperty(aggregate, 'entities', {
            get: () => getEntities(aggregate),
            enumerable: true,
            configurable: true
        });
    } catch (e) {
        // If property already exists and is not configurable, try to overwrite it
        (aggregate as any).entities = getEntities(aggregate);
    }

    Object.defineProperty(aggregate, 'methods', {
        get: () => getMethods(aggregate),
        enumerable: true,
        configurable: true
    });

    Object.defineProperty(aggregate, 'workflows', {
        get: () => getWorkflows(aggregate),
        enumerable: true,
        configurable: true
    });

    Object.defineProperty(aggregate, 'repository', {
        get: () => getRepository(aggregate),
        enumerable: true,
        configurable: true
    });

    Object.defineProperty(aggregate, 'events', {
        get: () => getEvents(aggregate),
        enumerable: true,
        configurable: true
    });

    Object.defineProperty(aggregate, 'references', {
        get: () => getReferences(aggregate),
        enumerable: true,
        configurable: true
    });

    Object.defineProperty(aggregate, 'webApiEndpoints', {
        get: () => getWebAPIEndpoints(aggregate),
        enumerable: true,
        configurable: true
    });

    Object.defineProperty(aggregate, 'serviceDefinition', {
        get: () => getServiceDefinition(aggregate),
        enumerable: true,
        configurable: true
    });

    Object.defineProperty(aggregate, 'functionalities', {
        get: () => getFunctionalities(aggregate),
        enumerable: true,
        configurable: true
    });

    initializedAggregates.add(aggregate);
}

// Export a function that can be used to safely access entities
export function getAggregateEntities(aggregate: Aggregate): Entity[] {
    ensureInitialized(aggregate);
    return aggregate.entities;
}

function lowerFirst(s: string): string {
    if (!s) return s;
    return s.charAt(0).toLowerCase() + s.slice(1);
}

export function getAggregateRefName(entity: Entity): string | undefined {
    const anyEntity = entity as any;
    const ref = anyEntity.aggregateRef;
    if (!ref) return undefined;
    if (typeof ref === 'string') return ref;
    return ref.ref?.name || ref.$refText;
}

/**
 * Returns the entity's explicit fieldMappings plus implicit base mappings (if applicable).
 *
 * Implicit base mappings are only added when the corresponding projection fields exist on the entity:
 *   <lower(aggregateRef)>AggregateId  -> aggregateId
 *   <lower(aggregateRef)>Version      -> version
 *   <lower(aggregateRef)>State        -> state
 *
 * This lets users declare the fields as plain properties without having to write boilerplate mappings
 * for fields inherited from the Aggregate base class.
 */
export function getEffectiveFieldMappings(entity: Entity): any[] {
    const anyEntity = entity as any;

    // New syntax: map dtoField as entityField
    const fieldMappings: any[] = Array.isArray(anyEntity.fieldMappings) ? anyEntity.fieldMappings : [];

    const aggregateRefName = getAggregateRefName(entity);
    if (!aggregateRefName) {
        return fieldMappings;
    }

    // Collect fields that exist on the entity (explicit properties + mapping-defined properties)
    const explicitProps = entity.properties || [];
    const mappingDefinedProps = fieldMappings
        .filter((m: any) => m?.type && m?.entityField)
        .map((m: any) => m.entityField);

    const existingEntityFields = new Set<string>([
        ...explicitProps.map((p: any) => p?.name).filter(Boolean),
        ...mappingDefinedProps.filter(Boolean),
    ]);

    const existingDtoFields = new Set<string>(
        fieldMappings
            .filter((m: any) => m?.dtoField)
            .map((m: any) => dtoFieldToString(m.dtoField))
    );
    const explicitlyMappedEntityFields = new Set<string>(fieldMappings.map((m: any) => m?.entityField).filter(Boolean));

    const prefix = lowerFirst(aggregateRefName);

    const pickBaseField = (dtoField: 'aggregateId' | 'version' | 'state', suffix: 'AggregateId' | 'Version' | 'State'): string | undefined => {
        if (existingDtoFields.has(dtoField)) return undefined;

        // 1) Prefer conventional name based on aggregateRef (e.g., userAggregateId)
        const conventional = `${prefix}${suffix}`;
        if (existingEntityFields.has(conventional) && !explicitlyMappedEntityFields.has(conventional)) {
            return conventional;
        }

        // 2) If there is exactly one remaining *<suffix>* field that isn't already mapped to something else,
        // use it. This supports names like "creatorAggregateId" or "executionAggregateId".
        const candidates = Array.from(existingEntityFields)
            .filter(name => typeof name === 'string' && name.endsWith(suffix))
            .filter(name => !explicitlyMappedEntityFields.has(name));

        if (candidates.length === 1) {
            return candidates[0];
        }

        // 3) If there is no unambiguous existing candidate, fall back to the conventional name.
        //    This allows us to synthesize both the mapping and the corresponding property
        //    (e.g., quizAggregateId) when the user doesn't declare it explicitly.
        return conventional;
    };

    const implicitBase = [
        { dtoField: 'aggregateId', suffix: 'AggregateId' },
        { dtoField: 'version', suffix: 'Version' },
        { dtoField: 'state', suffix: 'State' },
    ] as const;

    const implicitMappings = implicitBase
        .map(({ dtoField, suffix }) => {
            const entityField = pickBaseField(dtoField, suffix);
            if (!entityField) return null;

            // Add type for implicit base mappings
            let type: any;
            if (suffix === 'AggregateId' || suffix === 'Version') {
                type = 'Integer';
            } else if (suffix === 'State') {
                type = 'AggregateState';
            }

            return { entityField, dtoField, type };
        })
        .filter(Boolean) as any[];

    // Resolve types for explicit field mappings
    const explicitMappingsWithTypes = fieldMappings.map((mapping: any) => {
        if (mapping.type) {
            // Already has a type (shouldn't happen from DSL, but handle it)
            return mapping;
        }

        // Resolve type from referenced aggregate
        const resolvedType = resolveTypeFromReferencedAggregate(entity, mapping.dtoField);
        return {
            ...mapping,
            type: resolvedType
        };
    });

    return [...explicitMappingsWithTypes, ...implicitMappings];
}

/**
 * Helper: Convert DtoFieldPath to string representation
 * Examples:
 *   - "name" → "name"
 *   - { parts: ["questions", "aggregateId"] } → "questions.aggregateId"
 */
export function dtoFieldToString(dtoField: string | any): string {
    if (typeof dtoField === 'string') {
        return dtoField;
    }
    if (dtoField && Array.isArray(dtoField.parts)) {
        return dtoField.parts.join('.');
    }
    return String(dtoField);
}

/**
 * Helper: Extract element type from collection type (List<T>, Set<T>)
 * Returns { wrapper: 'List'|'Set', elementType: T } or undefined
 */
function extractCollectionElementType(collectionType: any): { wrapper: string; elementType: any } | undefined {
    if (!collectionType) return undefined;

    // Check for List<T> or Set<T> structure (unified type system)
    if (collectionType.$type === 'ListType') {
        return {
            wrapper: 'List',
            elementType: collectionType.elementType
        };
    }

    if (collectionType.$type === 'SetType') {
        return {
            wrapper: 'Set',
            elementType: collectionType.elementType
        };
    }

    return undefined;
}

/**
 * Helper: Find an entity type by name in all registered models
 */
function findEntityByName(entityTypeName: string): Entity | undefined {
    // Try cross-file resolution first
    if (allModelsRegistry.length > 0) {
        for (const model of allModelsRegistry) {
            if (!model.aggregates) continue;

            for (const aggregate of model.aggregates) {
                const entities = getEntities(aggregate);
                const entity = entities.find(e => e.name === entityTypeName);
                if (entity) return entity;
            }
        }
    }
    return undefined;
}

/**
 * Resolve the type of a field from a referenced aggregate's root entity.
 * Used for type inference in the new cross-aggregate syntax.
 *
 * This function supports CROSS-FILE type resolution by searching through all
 * registered models, not just the current file's model.
 *
 * Supports EXTRACT PATTERN: If dtoField is a dotted path (e.g., "questions.aggregateId"),
 * this function will:
 * 1. Find the collection property (questions) in the root entity
 * 2. Extract the element type (Question)
 * 3. Find the field (aggregateId) in the element type
 * 4. Wrap the result in the same collection type (List<Integer>)
 *
 * @param entity - The entity with the aggregateRef
 * @param dtoField - The field name to look up (can be dotted path for extraction)
 * @returns The type of the field, or undefined if not found
 */
function resolveTypeFromReferencedAggregate(entity: Entity, dtoField: string | any): any | undefined {
    const entityAny = entity as any;
    const aggregateRefName = entityAny.aggregateRef;

    if (!aggregateRefName) {
        return undefined;
    }

    // Handle dotted path (extract pattern: collection.field)
    // dtoField can be either a string or a DtoFieldPath object with parts array
    let fieldPath: string[];
    if (typeof dtoField === 'string') {
        fieldPath = [dtoField];
    } else if (dtoField && Array.isArray(dtoField.parts)) {
        // DtoFieldPath object from grammar: { parts: ['questions', 'aggregateId'] }
        fieldPath = dtoField.parts;
    } else {
        return undefined;
    }

    // CROSS-FILE RESOLUTION:
    // First, try to find the aggregate in ALL registered models (cross-file)
    let rootEntity: Entity | undefined;

    if (allModelsRegistry.length > 0) {
        for (const model of allModelsRegistry) {
            if (!model.aggregates) continue;

            const targetAggregate = model.aggregates.find(agg => agg.name === aggregateRefName);
            if (targetAggregate) {
                const entities = getEntities(targetAggregate);
                rootEntity = entities.find(e => e.isRoot);
                if (rootEntity) break;
            }
        }
    }

    // FALLBACK: Try current file's model (for backwards compatibility)
    if (!rootEntity) {
        const model = entity.$container?.$container as Model | undefined;
        if (!model || !model.aggregates) {
            return undefined;
        }

        const targetAggregate = model.aggregates.find(agg => agg.name === aggregateRefName);
        if (!targetAggregate) {
            return undefined;
        }

        const entities = getEntities(targetAggregate);
        rootEntity = entities.find(e => e.isRoot);
    }

    if (!rootEntity) {
        return undefined;
    }

    // Simple case: direct property lookup (e.g., "name")
    if (fieldPath.length === 1) {
        const property = rootEntity.properties?.find(p => p.name === fieldPath[0]);
        return property?.type;
    }

    // EXTRACT PATTERN: dotted path (e.g., "questions.aggregateId")
    // Step 1: Find the collection property
    const collectionProp = rootEntity.properties?.find(p => p.name === fieldPath[0]);
    if (!collectionProp || !collectionProp.type) {
        return undefined;
    }

    // Step 2: Extract element type from collection
    const collectionInfo = extractCollectionElementType(collectionProp.type);
    if (!collectionInfo) {
        return undefined; // Not a collection type
    }

    // Step 3: Resolve the element type to an entity
    let elementEntity: Entity | undefined;

    // Handle EntityType (reference to another entity)
    if (collectionInfo.elementType.$type === 'EntityType') {
        const entityTypeName = collectionInfo.elementType.type?.ref?.name || collectionInfo.elementType.type?.$refText;
        if (entityTypeName) {
            elementEntity = findEntityByName(entityTypeName);
        }
    }

    if (!elementEntity) {
        return undefined;
    }

    // Step 4: Find the target field in the element entity
    // Need to search in effective properties which includes mappings
    // The fieldPath[1] is a DTO field name (e.g., "aggregateId"), not an entity property name
    const effectiveProps = getEffectiveProperties(elementEntity);
    const targetProp = effectiveProps.find((p: any) => {
        // Check if this property's DTO field matches what we're looking for
        if (p.$dtoField && dtoFieldToString(p.$dtoField) === fieldPath[1]) {
            return true;
        }
        // Also check if the property name itself matches (for non-mapped fields)
        return p.name === fieldPath[1];
    });

    if (!targetProp || !targetProp.type) {
        return undefined;
    }

    // Step 5: Wrap the result in the same collection type (unified type system)
    return {
        $type: collectionInfo.wrapper === 'List' ? 'ListType' : 'SetType',
        elementType: targetProp.type
    };
}

/**
 * Get effective properties for an entity, including those defined in DTO mappings.
 * Supports type inference for new cross-aggregate syntax (map dtoField as entityField).
 * This function combines explicit properties with mapping-defined properties.
 */
export function getEffectiveProperties(entity: Entity): any[] {
    const explicitProps = entity.properties || [];
    const mappings = getEffectiveFieldMappings(entity);

    // Get properties defined in mappings
    const mappingProps = mappings
        .filter((m: any) => m.entityField)
        .map((m: any) => {
            let propertyType = m.type;

            // If no type specified (new syntax), infer from referenced aggregate
            if (!propertyType) {
                propertyType = resolveTypeFromReferencedAggregate(entity, m.dtoField);
            }

            // Skip if we couldn't resolve the type
            if (!propertyType) {
                return null;
            }

            return {
                name: m.entityField,
                type: propertyType,
                // Synthetic property from mapping
                $fromMapping: true,
                $dtoField: dtoFieldToString(m.dtoField),
                $extractField: m.extractField
            };
        })
        .filter(Boolean); // Remove null entries

    // Combine, avoiding duplicates (explicit props take precedence)
    const explicitNames = new Set(explicitProps.map(p => p.name));
    const combinedProps: any[] = [
        ...explicitProps,
        ...mappingProps.filter((p: any) => !explicitNames.has(p.name))
    ];

    const combinedNames = new Set(combinedProps.map(p => p.name));

    // Ensure base aggregate fields exist as synthetic properties when we created implicit mappings for them.
    // This drives validation and DTO generation even when the user doesn't declare these properties explicitly.
    for (const m of mappings) {
        if (!m?.entityField || !m?.dtoField) continue;
        if (combinedNames.has(m.entityField)) continue;

        const dtoFieldStr = dtoFieldToString(m.dtoField);
        let syntheticType: any | undefined;
        switch (dtoFieldStr) {
            case 'aggregateId':
            case 'version':
                syntheticType = { $type: 'PrimitiveType', typeName: 'Integer' };
                break;
            case 'state':
                syntheticType = { $type: 'BuiltinType', typeName: 'AggregateState' };
                break;
            default:
                break;
        }

        if (!syntheticType) continue;

        combinedProps.push({
            name: m.entityField,
            type: syntheticType,
            $syntheticBase: true,
            $dtoField: dtoFieldStr
        });
        combinedNames.add(m.entityField);
    }

    return combinedProps;
}
