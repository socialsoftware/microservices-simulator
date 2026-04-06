import { Aggregate, Entity, Method, Workflow, Repository, Events, References, WebAPIEndpoints, ServiceDefinition, Functionalities, isEntity, isMethod, isWorkflow, isRepository, isEvents, isReferences, isWebAPIEndpoints, isServiceDefinition, isFunctionalities, Model } from "../../language/generated/ast.js";







let allModelsRegistry: Model[] = [];



export function registerAllModels(models: Model[]): void {
    allModelsRegistry = models;
}



export function getAllModels(): Model[] {
    return allModelsRegistry;
}



export function clearModelsRegistry(): void {
    allModelsRegistry = [];
}

export interface PreventReferenceTo {
    sourceAggregateName: string;
    fieldName: string;
    message: string;
}

export function findPreventReferencesTo(targetAggregateName: string): PreventReferenceTo[] {
    const results: PreventReferenceTo[] = [];
    for (const model of allModelsRegistry) {
        if (!model.aggregates) continue;
        for (const aggregate of model.aggregates) {
            const refs = (aggregate as any).references;
            if (!refs || !refs.constraints) continue;
            for (const constraint of refs.constraints) {
                if (constraint.targetAggregate === targetAggregateName && constraint.action === 'prevent') {
                    results.push({
                        sourceAggregateName: aggregate.name,
                        fieldName: constraint.fieldName,
                        message: constraint.message
                    });
                }
            }
        }
    }
    return results;
}





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


function ensureInitialized(aggregate: Aggregate): void {
    if (!initializedAggregates.has(aggregate)) {
        initializeAggregateProperties(aggregate);
    }
}

export function initializeAggregateProperties(aggregate: Aggregate): void {
    if (initializedAggregates.has(aggregate)) {
        return;
    }

    
    if (!aggregate.aggregateElements) {
        (aggregate as any).aggregateElements = [];
    }

    
    try {
        Object.defineProperty(aggregate, 'entities', {
            get: () => getEntities(aggregate),
            enumerable: true,
            configurable: true
        });
    } catch (e) {
        
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



export function getEffectiveFieldMappings(entity: Entity): any[] {
    const anyEntity = entity as any;

    
    const fieldMappings: any[] = Array.isArray(anyEntity.fieldMappings) ? anyEntity.fieldMappings : [];

    const aggregateRefName = getAggregateRefName(entity);
    if (!aggregateRefName) {
        return fieldMappings;
    }

    
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

        
        const conventional = `${prefix}${suffix}`;
        if (existingEntityFields.has(conventional) && !explicitlyMappedEntityFields.has(conventional)) {
            return conventional;
        }

        
        
        const candidates = Array.from(existingEntityFields)
            .filter(name => typeof name === 'string' && name.endsWith(suffix))
            .filter(name => !explicitlyMappedEntityFields.has(name));

        if (candidates.length === 1) {
            return candidates[0];
        }

        
        
        
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

            
            let type: any;
            if (suffix === 'AggregateId' || suffix === 'Version') {
                type = 'Integer';
            } else if (suffix === 'State') {
                type = 'AggregateState';
            }

            return { entityField, dtoField, type };
        })
        .filter(Boolean) as any[];

    
    const explicitMappingsWithTypes = fieldMappings.map((mapping: any) => {
        if (mapping.type) {
            
            return mapping;
        }

        
        const resolvedType = resolveTypeFromReferencedAggregate(entity, mapping.dtoField);
        return {
            ...mapping,
            type: resolvedType
        };
    });

    return [...explicitMappingsWithTypes, ...implicitMappings];
}



export function dtoFieldToString(dtoField: string | any): string {
    if (typeof dtoField === 'string') {
        return dtoField;
    }
    if (dtoField && Array.isArray(dtoField.parts)) {
        return dtoField.parts.join('.');
    }
    return String(dtoField);
}



function extractCollectionElementType(collectionType: any): { wrapper: string; elementType: any } | undefined {
    if (!collectionType) return undefined;

    
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



function findEntityByName(entityTypeName: string): Entity | undefined {
    
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



function resolveTypeFromReferencedAggregate(entity: Entity, dtoField: string | any): any | undefined {
    const entityAny = entity as any;
    const aggregateRefName = entityAny.aggregateRef;

    if (!aggregateRefName) {
        return undefined;
    }

    
    
    let fieldPath: string[];
    if (typeof dtoField === 'string') {
        fieldPath = [dtoField];
    } else if (dtoField && Array.isArray(dtoField.parts)) {
        
        fieldPath = dtoField.parts;
    } else {
        return undefined;
    }

    
    
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

    
    if (fieldPath.length === 1) {
        const property = rootEntity.properties?.find(p => p.name === fieldPath[0]);
        return property?.type;
    }

    
    
    const collectionProp = rootEntity.properties?.find(p => p.name === fieldPath[0]);
    if (!collectionProp || !collectionProp.type) {
        return undefined;
    }

    
    const collectionInfo = extractCollectionElementType(collectionProp.type);
    if (!collectionInfo) {
        return undefined; 
    }

    
    let elementEntity: Entity | undefined;

    
    if (collectionInfo.elementType.$type === 'EntityType') {
        const entityTypeName = collectionInfo.elementType.type?.ref?.name || collectionInfo.elementType.type?.$refText;
        if (entityTypeName) {
            elementEntity = findEntityByName(entityTypeName);
        }
    }

    if (!elementEntity) {
        return undefined;
    }

    
    
    
    const effectiveProps = getEffectiveProperties(elementEntity);
    const targetProp = effectiveProps.find((p: any) => {
        
        if (p.$dtoField && dtoFieldToString(p.$dtoField) === fieldPath[1]) {
            return true;
        }
        
        return p.name === fieldPath[1];
    });

    if (!targetProp || !targetProp.type) {
        return undefined;
    }

    
    return {
        $type: collectionInfo.wrapper === 'List' ? 'ListType' : 'SetType',
        elementType: targetProp.type
    };
}



export function getEffectiveProperties(entity: Entity): any[] {
    const explicitProps = entity.properties || [];
    const mappings = getEffectiveFieldMappings(entity);

    
    const mappingProps = mappings
        .filter((m: any) => m.entityField)
        .map((m: any) => {
            let propertyType = m.type;

            
            if (!propertyType) {
                propertyType = resolveTypeFromReferencedAggregate(entity, m.dtoField);
            }

            
            if (!propertyType) {
                return null;
            }

            return {
                name: m.entityField,
                type: propertyType,
                
                $fromMapping: true,
                $dtoField: dtoFieldToString(m.dtoField),
                $extractField: m.extractField
            };
        })
        .filter(Boolean); 

    
    const explicitNames = new Set(explicitProps.map(p => p.name));
    const combinedProps: any[] = [
        ...explicitProps,
        ...mappingProps.filter((p: any) => !explicitNames.has(p.name))
    ];

    const combinedNames = new Set(combinedProps.map(p => p.name));

    
    
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
