import { Entity } from "../../../../../language/generated/ast.js";

export interface EventGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export interface EventContext {
    aggregateName: string;
    capitalizedAggregate: string;
    lowerAggregate: string;
    packageName: string;
    rootEntity: Entity;
    projectName: string;
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
