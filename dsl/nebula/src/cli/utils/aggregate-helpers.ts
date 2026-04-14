import { Aggregate, Entity, Method, Workflow, Repository, Events, ServiceDefinition, Functionalities, isEntity, isMethod, isWorkflow, isRepository, isEvents, isServiceDefinition, isFunctionalities, Model } from "../../language/generated/ast.js";







let allModelsRegistry: Model[] = [];



export function registerAllModels(models: Model[]): void {
    allModelsRegistry = models;
}



export function getAllModels(): Model[] {
    return allModelsRegistry;
}




export function resolveUltimateSourceRoot(name: string, seen: Set<string> = new Set()): Entity | undefined {
    if (seen.has(name)) return undefined;
    seen.add(name);

    for (const model of allModelsRegistry) {
        if (!model.aggregates) continue;
        const matchedAggregate = model.aggregates.find(agg => agg.name === name);
        if (matchedAggregate) {
            const root = getEntities(matchedAggregate).find(e => (e as any).isRoot);
            if (root) return root;
        }
    }

    for (const model of allModelsRegistry) {
        if (!model.aggregates) continue;
        for (const aggregate of model.aggregates) {
            const nested = getEntities(aggregate).find(e => e.name === name && !(e as any).isRoot);
            if (nested) {
                const nestedRef = (nested as any).aggregateRef;
                if (nestedRef) {
                    return resolveUltimateSourceRoot(nestedRef, seen);
                }
                return undefined;
            }
        }
    }

    return undefined;
}






export function getEntities(aggregate: Aggregate): Entity[] {
    if (!aggregate.aggregateElements || !Array.isArray(aggregate.aggregateElements)) {
        return [];
    }
    return aggregate.aggregateElements.filter((el): el is Entity => isEntity(el));
}

export function getMethods(aggregate: Aggregate): Method[] {
    const result: Method[] = [];
    for (const el of aggregate.aggregateElements || []) {
        if ((el as any).$type === 'Methods') {
            const nested = (el as any).methods || [];
            for (const m of nested) {
                if (isMethod(m)) result.push(m);
            }
        }
    }
    return result;
}

export function getWorkflows(aggregate: Aggregate): Workflow[] {
    return aggregate.aggregateElements.filter((el): el is Workflow => isWorkflow(el));
}

export function getRepository(aggregate: Aggregate): Repository | undefined {
    return aggregate.aggregateElements.find((el): el is Repository => isRepository(el));
}

export function getEvents(aggregate: Aggregate): Events | undefined {
    const explicit = aggregate.aggregateElements.find((el): el is Events => isEvents(el));
    const inferred = inferPublishedEventsFromPublishesClauses(aggregate);

    const allSubs = ((explicit as any)?.subscribedEvents || []) as any[];
    const invariantSubs = allSubs.filter(s => !!s.invariantName);
    const plainSubs = allSubs.filter(s => !s.invariantName);
    const byName = new Map<string, any[]>();
    for (const s of invariantSubs) {
        if (!byName.has(s.invariantName)) byName.set(s.invariantName, []);
        byName.get(s.invariantName)!.push(Object.assign({}, s, {
            conditions: s.invariantCondition ? [{ condition: s.invariantCondition }] : []
        }));
    }
    const syntheticInvariants = Array.from(byName.entries()).map(([name, subs]) => ({
        $type: 'InterInvariant',
        name,
        subscribedEvents: subs
    }));

    if (inferred.length === 0 && syntheticInvariants.length === 0) {
        if (!explicit) return explicit;
        const shim: any = Object.create(explicit);
        Object.assign(shim, explicit);
        shim.subscribedEvents = plainSubs;
        shim.interInvariants = [];
        return shim as Events;
    }

    const explicitPublished = (explicit?.publishedEvents || []) as any[];
    const explicitNames = new Set(explicitPublished.map((e: any) => e.name));
    const newOnes = inferred.filter(e => !explicitNames.has(e.name));

    const merged: any = explicit ? Object.create(explicit) : { $type: 'Events' };
    Object.assign(merged, explicit || {});
    merged.publishedEvents = [...explicitPublished, ...newOnes];
    merged.subscribedEvents = plainSubs;
    merged.interInvariants = syntheticInvariants;
    return merged as Events;
}

function inferPublishedEventsFromPublishesClauses(aggregate: Aggregate): any[] {
    const eventMap = new Map<string, { name: string; fields: Map<string, any> }>();

    const collectFromClause = (clause: any, contextMethod?: any) => {
        if (!clause || !clause.eventType) return;
        const eventName = clause.eventType;
        let entry = eventMap.get(eventName);
        if (!entry) {
            entry = { name: eventName, fields: new Map() };
            eventMap.set(eventName, entry);
        }
        for (const a of (clause.assignments || [])) {
            if (!a?.field) continue;
            if (entry.fields.has(a.field)) continue;
            const inferredType = inferAssignmentType(a.value, contextMethod, aggregate);
            entry.fields.set(a.field, inferredType);
        }
    };

    for (const method of getMethods(aggregate)) {
        const ab: any = (method as any).actionBody;
        for (const pub of (ab?.publishes || [])) {
            collectFromClause(pub, method);
        }
    }

    const result: any[] = [];
    for (const [name, entry] of eventMap) {
        result.push({
            $type: 'PublishedEvent',
            name,
            fields: Array.from(entry.fields.entries()).map(([fieldName, type]) => ({
                $type: 'EventField',
                name: fieldName,
                type,
            })),
        });
    }
    return result;
}

function inferAssignmentType(expr: any, method: any, aggregate: Aggregate): any {
    if (!expr) return makeBuiltinType('Object');

    if (expr.$type === 'ActionLiteral') {
        if (expr.stringValue !== undefined) return makeBuiltinType('String');
        if (expr.boolLiteral !== undefined) return makeBuiltinType('Boolean');
        if (expr.literalValue !== undefined) {
            const v = expr.literalValue;
            if (typeof v === 'string' && /\./.test(v)) return makeBuiltinType('Double');
            return makeBuiltinType('Integer');
        }
        return makeBuiltinType('Object');
    }

    if (expr.$type === 'ActionRef') {
        const head: string = expr.name;
        const chain: string[] = expr.chain || [];

        if (chain.length === 0) {
            const param = (method?.parameters || []).find((p: any) => p?.name === head);
            if (param?.type) return param.type;
            return makeBuiltinType('Object');
        }

        const last = chain[chain.length - 1];
        if (last === 'aggregateId' || last === 'version') return makeBuiltinType('Integer');
        if (last === 'state') return makeBuiltinType('AggregateState');
        if (last.endsWith('AggregateId') || last.endsWith('Version')) return makeBuiltinType('Integer');
        if (last.endsWith('State')) return makeBuiltinType('AggregateState');

        const entity = resolveAliasOrParamEntity(head, method, aggregate);
        if (entity) {
            let current: any = entity;
            for (const c of chain) {
                const prop = (current?.properties || []).find((p: any) => p?.name === c);
                if (!prop) { current = null; break; }
                if (c === chain[chain.length - 1]) return prop.type;
                const refName = prop.type?.type?.$refText || prop.type?.type?.ref?.name;
                if (refName) {
                    current = (aggregate.aggregateElements || []).find(
                        (e: any) => e.$type === 'Entity' && e.name === refName
                    );
                } else {
                    current = null;
                }
            }
        }
        return makeBuiltinType('Object');
    }

    return makeBuiltinType('Object');
}

function resolveAliasOrParamEntity(name: string, method: any, aggregate: Aggregate): any {
    for (const stmt of (method?.actionBody?.statements || [])) {
        if (stmt?.$type === 'LoadActionStatement' && stmt.alias === name) {
            return (aggregate.aggregateElements || []).find(
                (e: any) => e.$type === 'Entity' && e.name === stmt.aggregateRef
            );
        }
    }
    const param = (method?.parameters || []).find((p: any) => p?.name === name);
    if (param?.type?.$type === 'EntityType') {
        const refName = param.type.type?.$refText || param.type.type?.ref?.name;
        if (refName) {
            return (aggregate.aggregateElements || []).find(
                (e: any) => e.$type === 'Entity' && e.name === refName
            );
        }
    }
    const rootEntity = (aggregate.aggregateElements || []).find(
        (e: any) => e.$type === 'Entity' && e.isRoot
    ) as any;
    if (rootEntity && (name === rootEntity.name?.toLowerCase() || name === aggregate.name?.toLowerCase())) {
        return rootEntity;
    }
    return undefined;
}

function makeBuiltinType(name: string): any {
    return { $type: 'PrimitiveType', typeName: name };
}

export function getWebAPIEndpoints(aggregate: Aggregate): any | undefined {
    const synthesized = synthesizeEndpointsFromMethodAnnotations(aggregate);
    if (synthesized.length === 0) {
        return undefined;
    }
    return { $type: 'WebAPIEndpoints', endpoints: synthesized };
}

const HTTP_VERBS: Record<string, string> = {
    GetMapping: 'GET',
    PostMapping: 'POST',
    PutMapping: 'PUT',
    DeleteMapping: 'DELETE',
    PatchMapping: 'PATCH',
};

function synthesizeEndpointsFromMethodAnnotations(aggregate: Aggregate): any[] {
    const endpoints: any[] = [];
    for (const method of getMethods(aggregate)) {
        const mappingAnno = (method as any).annotations?.find((a: any) => HTTP_VERBS[a.name]);
        if (!mappingAnno) continue;

        const verb = HTTP_VERBS[mappingAnno.name];
        const rawPath = (mappingAnno.values || [])[0]?.value;
        if (typeof rawPath !== 'string') continue;
        const path = rawPath.replace(/^["']|["']$/g, '');

        const pathVarNames = new Set<string>();
        const pathVarRegex = /\{([a-zA-Z_][a-zA-Z0-9_]*)\}/g;
        let m: RegExpExecArray | null;
        while ((m = pathVarRegex.exec(path)) !== null) {
            pathVarNames.add(m[1]);
        }

        const parameters = (method.parameters || []).map((p: any) => {
            const explicitAnno = (p.annotations || []).find((a: any) =>
                ['PathVariable', 'RequestParam', 'RequestBody', 'RequestHeader'].includes(a.name)
            );
            let annotation: string;
            if (explicitAnno) {
                annotation = `@${explicitAnno.name}`;
            } else if (pathVarNames.has(p.name)) {
                annotation = '@PathVariable';
            } else if (p.type?.$type === 'EntityType') {
                annotation = '@RequestBody';
            } else {
                annotation = '@RequestParam';
            }
            return { name: p.name, type: p.type, annotation };
        });

        endpoints.push({
            $type: 'CustomEndpoint',
            name: method.name,
            httpMethod: { $type: 'HttpMethod', method: verb },
            path,
            methodName: method.name,
            parameters,
            returnType: method.returnType,
            description: undefined,
            throwsException: undefined,
        });
    }
    return endpoints;
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
        webApiEndpoints?: any;
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

    for (const element of aggregate.aggregateElements || []) {
        const entity = element as any;
        if (element.$type === 'Entity') {
            if (entity.sourceBlocks && entity.sourceBlocks.length > 0 && !entity.aggregateRef) {
                const block = entity.sourceBlocks[0];
                entity.aggregateRef = block.aggregateRef;
                if (!entity.fieldMappings || entity.fieldMappings.length === 0) {
                    entity.fieldMappings = (block.sources || []).map((s: any) => {
                        const parts = s.dtoField?.parts || [];
                        return {
                            dtoField: s.dtoField,
                            entityField: s.entityField || (parts.length === 1 ? parts[0] : undefined),
                            type: s.type
                        };
                    });
                }
            }
        }
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



export function findEntityByName(entityTypeName: string): Entity | undefined {

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



    const rootEntity = resolveUltimateSourceRoot(aggregateRefName);
    if (!rootEntity) {
        return undefined;
    }


    if (fieldPath.length === 1) {
        const property = rootEntity.properties?.find(p => p.name === fieldPath[0]);
        if (property) {
            return property.type;
        }

        for (const nestedProp of rootEntity.properties || []) {
            const nestedJavaType = (nestedProp.type as any)?.type?.ref?.name || (nestedProp.type as any)?.type?.$refText;
            if (!nestedJavaType) continue;
            const nestedEntity = findEntityByName(nestedJavaType);
            if (!nestedEntity) continue;
            const innerProps = getEffectiveProperties(nestedEntity);
            const innerProp = innerProps.find((p: any) => p.name === fieldPath[0]);
            if (innerProp) {
                return innerProp.type;
            }
        }

        return undefined;
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

    const anyEntity = entity as any;
    const aggregateRef = anyEntity.aggregateRef;
    if (aggregateRef && !anyEntity.isRoot) {
        const refName = typeof aggregateRef === 'string' ? aggregateRef :
            aggregateRef.ref?.name || aggregateRef.$refText || '';
        const lowerRef = refName.charAt(0).toLowerCase() + refName.slice(1);

        const idFieldName = `${lowerRef}AggregateId`;
        const versionFieldName = `${lowerRef}Version`;
        const stateFieldName = `${lowerRef}State`;

        if (!combinedNames.has(idFieldName)) {
            combinedProps.push({
                name: idFieldName,
                type: { $type: 'PrimitiveType', typeName: 'Integer' },
                $syntheticBase: true
            });
            combinedNames.add(idFieldName);
        }
        if (!combinedNames.has(versionFieldName)) {
            combinedProps.push({
                name: versionFieldName,
                type: { $type: 'PrimitiveType', typeName: 'Integer' },
                $syntheticBase: true
            });
            combinedNames.add(versionFieldName);
        }
        if (!combinedNames.has(stateFieldName)) {
            combinedProps.push({
                name: stateFieldName,
                type: { $type: 'BuiltinType', typeName: 'AggregateState' },
                $syntheticBase: true
            });
            combinedNames.add(stateFieldName);
        }
    }

    return combinedProps;
}
