


import {
    Aggregate,
    Entity,
    Property,
    Repository,
    ServiceDefinition,
    Invariant
} from '../../language/generated/ast.js';



export interface AggregateExt extends Aggregate {
    

    rootEntity?: EntityExt;
}



export interface EntityExt extends Entity {
    

    aggregate?: AggregateExt;

    

    eventSubscriptions?: string[];
}



export interface PropertyExt extends Property {
    

    jpaRelation?: 'OneToOne' | 'OneToMany' | 'ManyToOne' | 'ManyToMany';

    

    jpaCascade?: string[];

    

    elementType?: string;

    

    isCollection?: boolean;
}



export interface EventsExt {
    

    publishedEvents: PublishedEventExt[];

    

    subscribedEvents: SubscribedEventExt[];

    

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



export interface InvariantExt extends Invariant {
    name: string;
    expression: string;
    errorMessage: string;
}



export interface InterInvariantExt {
    name: string;
    subscribedEvents: any[];
    condition?: string;
    affectedAggregates?: string[];
}



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



export interface ServiceDefinitionExt extends ServiceDefinition {
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



export interface MethodDefinition {
    name: string;
    returnType: string;
    parameters: MethodParameterExt[];
    body: string;
    visibility: 'public' | 'protected' | 'private';
    annotations?: string[];
}



export namespace TypeGuards {
    

    export function isAggregateExt(obj: any): obj is AggregateExt {
        return obj && obj.$type === 'Aggregate';
    }

    

    export function isEntityExt(obj: any): obj is EntityExt {
        return obj && obj.$type === 'Entity';
    }

    

    export function isPropertyExt(obj: any): obj is PropertyExt {
        return obj && obj.$type === 'Property';
    }

    

    export function isRootEntity(entity: EntityExt): boolean {
        return entity.isRoot === true;
    }

    

    export function hasInvariants(entity: EntityExt): boolean {
        return entity.invariants !== undefined && entity.invariants.length > 0;
    }

    

    export function hasEventSubscriptions(entity: EntityExt): boolean {
        return entity.eventSubscriptions !== undefined && entity.eventSubscriptions.length > 0;
    }

    

    export function hasEvents(aggregate: AggregateExt): boolean {
        return aggregate.events !== undefined;
    }

    

    export function hasCustomRepository(aggregate: AggregateExt): boolean {
        return aggregate.repository !== undefined;
    }

    

    export function isCollectionProperty(property: PropertyExt): boolean {
        return property.isCollection === true;
    }

    

    export function isOptionalProperty(property: Property): boolean {
        
        return property.type?.$type === 'OptionalType';
    }
}
