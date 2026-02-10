import { Entity, Aggregate } from "../../../../language/generated/ast.js";

export interface EventGenerationOptions {
    architecture?: string;
    projectName: string;
    basePackage: string;
    allAggregates?: Aggregate[];
}

export interface EventContext {
    aggregateName: string;
    capitalizedAggregate: string;
    lowerAggregate: string;
    packageName: string;
    rootEntity: Entity;
    projectName: string;
    basePackage: string;
    imports: string[];
}

export interface EventHandlerContext extends EventContext {
    eventHandlers: any[];
    isAbstract?: boolean;
}

export interface PublishedEventContext extends EventContext {
    publishedEvents: any[];
}

export interface EventSubscriptionContext extends EventContext {
    eventSubscriptions: any[];
}
