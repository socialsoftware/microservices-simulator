/**
 * AST Extension Types
 *
 * This module provides typed extensions to Langium-generated AST types,
 * adding runtime metadata and type safety across all generators.
 */

import {
    Aggregate,
    Entity,
    Property,
    Repository,
    ServiceDefinition,
    WebAPIEndpoints,
    Invariant,
    InterInvariant
} from '../../language/generated/ast.js';

/**
 * Extended Aggregate interface with runtime metadata
 */
export interface AggregateExt extends Aggregate {
    /** Root entity reference (computed) */
    rootEntity?: EntityExt;
}

/**
 * Extended Entity interface with metadata
 */
export interface EntityExt extends Entity {
    /** Parent aggregate reference (computed) */
    aggregate?: AggregateExt;

    /** Event subscriptions (computed) */
    eventSubscriptions?: string[];
}

/**
 * Extended Property interface with JPA metadata
 */
export interface PropertyExt extends Property {
    /** JPA relationship type (computed) */
    jpaRelation?: 'OneToOne' | 'OneToMany' | 'ManyToOne' | 'ManyToMany';

    /** JPA cascade types (computed) */
    jpaCascade?: string[];

    /** Element type for collections (computed) */
    elementType?: string;

    /** Whether this is a collection (computed) */
    isCollection?: boolean;
}

/**
 * Extended Events interface
 * Note: Does not extend Events to avoid type conflicts with required arrays
 */
export interface EventsExt {
    /** Published events */
    publishedEvents: PublishedEventExt[];

    /** Subscribed events */
    subscribedEvents: SubscribedEventExt[];

    /** Inter-aggregate invariants */
    interInvariants: InterInvariantExt[];
}

export interface PublishedEventExt {
    name: string;
    fields: EventFieldExt[];
}

export interface SubscribedEventExt {
    name: string;
    handlerName?: string;
    eventClass?: string;
}

export interface EventFieldExt {
    name: string;
    type: string;
}

/**
 * Extended Invariant interface
 */
export interface InvariantExt extends Invariant {
    name: string;
    expression: string;
    errorMessage: string;
}

/**
 * Extended InterInvariant interface
 */
export interface InterInvariantExt extends InterInvariant {
    name: string;
    condition: string;
    affectedAggregates: string[];
}

/**
 * Extended Repository interface
 */
export interface RepositoryExt extends Repository {
    customQueries: CustomQueryExt[];
}

export interface CustomQueryExt {
    name: string;
    query: string;
    returnType: string;
    parameters: QueryParameterExt[];
}

export interface QueryParameterExt {
    name: string;
    type: string;
}

/**
 * Extended Service Definition
 */
export interface ServiceDefinitionExt extends ServiceDefinition {
    generateCrud: boolean;
    customMethods: ServiceMethodExt[];
}

export interface ServiceMethodExt {
    name: string;
    returnType: string;
    parameters: MethodParameterExt[];
    body?: string;
}

export interface MethodParameterExt {
    name: string;
    type: string;
    annotations: string[];
}

/**
 * Extended Web API Endpoints
 */
export interface WebAPIEndpointsExt extends WebAPIEndpoints {
    generateCrud: boolean;
    customEndpoints: EndpointExt[];
}

export interface EndpointExt {
    httpMethod: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    path: string;
    methodName: string;
    parameters: EndpointParameterExt[];
    returnType: string;
}

export interface EndpointParameterExt {
    name: string;
    type: string;
    source: 'PathVariable' | 'RequestParam' | 'RequestBody';
}

/**
 * Method definition
 */
export interface MethodDefinition {
    name: string;
    returnType: string;
    parameters: MethodParameterExt[];
    body: string;
    visibility: 'public' | 'protected' | 'private';
    annotations?: string[];
}

/**
 * Type guards for runtime type checking
 */
export namespace TypeGuards {
    /**
     * Check if object is an AggregateExt
     */
    export function isAggregateExt(obj: any): obj is AggregateExt {
        return obj && obj.$type === 'Aggregate';
    }

    /**
     * Check if object is an EntityExt
     */
    export function isEntityExt(obj: any): obj is EntityExt {
        return obj && obj.$type === 'Entity';
    }

    /**
     * Check if object is a PropertyExt
     */
    export function isPropertyExt(obj: any): obj is PropertyExt {
        return obj && obj.$type === 'Property';
    }

    /**
     * Check if entity is a root entity
     */
    export function isRootEntity(entity: EntityExt): boolean {
        return entity.isRoot === true;
    }

    /**
     * Check if entity has invariants defined
     */
    export function hasInvariants(entity: EntityExt): boolean {
        return entity.invariants !== undefined && entity.invariants.length > 0;
    }

    /**
     * Check if entity has event subscriptions
     */
    export function hasEventSubscriptions(entity: EntityExt): boolean {
        return entity.eventSubscriptions !== undefined && entity.eventSubscriptions.length > 0;
    }

    /**
     * Check if aggregate has events
     */
    export function hasEvents(aggregate: AggregateExt): boolean {
        return aggregate.events !== undefined;
    }

    /**
     * Check if aggregate has custom repository
     */
    export function hasCustomRepository(aggregate: AggregateExt): boolean {
        return aggregate.repository !== undefined;
    }

    /**
     * Check if property is a collection type
     */
    export function isCollectionProperty(property: PropertyExt): boolean {
        return property.isCollection === true;
    }

    /**
     * Check if property is optional
     */
    export function isOptionalProperty(property: Property): boolean {
        // Check if property type is Optional<T>
        return property.type?.$type === 'OptionalType';
    }
}
