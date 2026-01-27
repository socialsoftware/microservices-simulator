import { Aggregate, Entity, Method, Workflow, Repository, Events, WebAPIEndpoints, ServiceDefinition, Functionalities, isEntity, isMethod, isWorkflow, isRepository, isEvents, isWebAPIEndpoints, isServiceDefinition, isFunctionalities } from "../../language/generated/ast.js";

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

function getAggregateRefName(entity: Entity): string | undefined {
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
    const explicitMappings: any[] = Array.isArray(anyEntity.fieldMappings) ? anyEntity.fieldMappings : [];

    const aggregateRefName = getAggregateRefName(entity);
    if (!aggregateRefName) {
        return explicitMappings;
    }

    // Collect fields that exist on the entity (explicit properties + mapping-defined properties)
    const explicitProps = entity.properties || [];
    const mappingDefinedProps = explicitMappings
        .filter((m: any) => m?.type && m?.entityField)
        .map((m: any) => m.entityField);

    const existingEntityFields = new Set<string>([
        ...explicitProps.map((p: any) => p?.name).filter(Boolean),
        ...mappingDefinedProps.filter(Boolean),
    ]);

    const existingDtoFields = new Set<string>(explicitMappings.map((m: any) => m?.dtoField).filter(Boolean));
    const explicitlyMappedEntityFields = new Set<string>(explicitMappings.map((m: any) => m?.entityField).filter(Boolean));

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

        return undefined;
    };

    const implicitBase = [
        { dtoField: 'aggregateId', suffix: 'AggregateId' },
        { dtoField: 'version', suffix: 'Version' },
        { dtoField: 'state', suffix: 'State' },
    ] as const;

    const implicitMappings = implicitBase
        .map(({ dtoField, suffix }) => {
            const entityField = pickBaseField(dtoField, suffix);
            return entityField ? { entityField, dtoField } : null;
        })
        .filter(Boolean) as any[];

    return [...explicitMappings, ...implicitMappings];
}

/**
 * Get effective properties for an entity, including those defined in DTO mappings.
 * With the simplified mapping syntax, properties can be defined inline:
 *   Integer courseAggregateId -> aggregateId;
 * This function combines explicit properties with mapping-defined properties.
 */
export function getEffectiveProperties(entity: Entity): any[] {
    const explicitProps = entity.properties || [];
    const mappings = getEffectiveFieldMappings(entity);

    // Get properties defined in mappings (new syntax with type)
    const mappingProps = mappings
        .filter((m: any) => m.type && m.entityField)
        .map((m: any) => ({
            name: m.entityField,
            type: m.type,
            // Synthetic property from mapping
            $fromMapping: true,
            $dtoField: m.dtoField,
            $extractField: m.extractField
        }));

    // Combine, avoiding duplicates (explicit props take precedence)
    const explicitNames = new Set(explicitProps.map(p => p.name));
    const combinedProps = [
        ...explicitProps,
        ...mappingProps.filter((p: any) => !explicitNames.has(p.name))
    ];

    return combinedProps;
}
