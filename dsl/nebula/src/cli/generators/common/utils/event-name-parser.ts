/**
 * Event Name Parser - Centralized event name parsing utilities
 *
 * This module provides consistent parsing of event names across the codebase.
 * Replaces 25+ duplicate regex patterns scattered across generators.
 */

/**
 * CRUD action types that can be extracted from event names
 */
export type EventAction = 'create' | 'update' | 'delete' | 'unknown';

/**
 * Result of parsing an event name
 */
export interface ParsedEventName {
    /** Full event name including "Event" suffix (e.g., "UserUpdatedEvent") */
    fullName: string;
    /** Event name without "Event" suffix (e.g., "UserUpdated") */
    baseName: string;
    /** Entity/Aggregate name (e.g., "User") */
    entityName: string;
    /** Action type if present */
    action: EventAction;
    /** Publisher aggregate name (for CRUD events, same as entityName) */
    publisherAggregate: string;
}

/**
 * Centralized event name parser
 */
export class EventNameParser {
    /**
     * Remove "Event" suffix from event name
     * @example removeEventSuffix("UserUpdatedEvent") → "UserUpdated"
     */
    static removeEventSuffix(eventName: string): string {
        return eventName.replace(/Event$/, '');
    }

    /**
     * Extract entity name from event name by removing action prefix and "Event" suffix
     * @example extractEntityName("UpdateUserEvent") → "User"
     * @example extractEntityName("UserUpdatedEvent") → "User"
     */
    static extractEntityName(eventName: string): string {
        // Remove prefix (Create/Update/Delete) and suffix (Event)
        let result = eventName.replace(/^(Update|Delete|Create)/, '');

        // Remove action suffix (Created/Updated/Deleted) and Event suffix
        result = result.replace(/(Updated|Deleted|Created)Event$/, '');

        // Fallback: just remove Event suffix if no action found
        if (result === eventName) {
            result = eventName.replace(/Event$/, '');
        }

        return result;
    }

    /**
     * Extract action from event name
     * @example extractAction("UserCreatedEvent") → "create"
     * @example extractAction("UserUpdatedEvent") → "update"
     * @example extractAction("UserDeletedEvent") → "delete"
     */
    static extractAction(eventName: string): EventAction {
        if (eventName.match(/Created?Event$/)) return 'create';
        if (eventName.match(/Updated?Event$/)) return 'update';
        if (eventName.match(/Deleted?Event$/)) return 'delete';
        return 'unknown';
    }

    /**
     * Extract publisher aggregate name from event name
     * For CRUD events, this is the same as entity name
     * @example extractPublisherAggregate("UserUpdatedEvent") → "User"
     */
    static extractPublisherAggregate(eventName: string): string {
        // Remove optional action (Created/Updated/Deleted) and Event suffix
        return eventName.replace(/(Deleted|Updated|Created)?Event$/, '');
    }

    /**
     * Check if event name represents a CRUD event (Created/Updated/Deleted)
     */
    static isCrudEvent(eventName: string): boolean {
        return eventName.match(/(Created|Updated|Deleted)Event$/) !== null;
    }

    /**
     * Check if event name represents an Update event
     */
    static isUpdateEvent(eventName: string): boolean {
        return eventName.match(/Updated?Event$/) !== null;
    }

    /**
     * Check if event name represents a Delete event
     */
    static isDeleteEvent(eventName: string): boolean {
        return eventName.match(/Deleted?Event$/) !== null;
    }

    /**
     * Check if event name represents a Create event
     */
    static isCreateEvent(eventName: string): boolean {
        return eventName.match(/Created?Event$/) !== null;
    }

    /**
     * Parse event name into all components
     * @example parse("UserUpdatedEvent") → { fullName: "UserUpdatedEvent", baseName: "UserUpdated", entityName: "User", action: "update", publisherAggregate: "User" }
     */
    static parse(eventName: string): ParsedEventName {
        return {
            fullName: eventName,
            baseName: this.removeEventSuffix(eventName),
            entityName: this.extractEntityName(eventName),
            action: this.extractAction(eventName),
            publisherAggregate: this.extractPublisherAggregate(eventName)
        };
    }

    /**
     * Generate subscription class name from event name and aggregate name
     * @example generateSubscriptionName("User", "ExecutionDeletedEvent") → "UserSubscribesExecutionDeleted"
     */
    static generateSubscriptionName(aggregateName: string, eventName: string): string {
        const eventWithoutSuffix = this.removeEventSuffix(eventName);
        return `${aggregateName}Subscribes${eventWithoutSuffix}`;
    }

    /**
     * Generate handler method name from event name
     * @example generateHandlerMethodName("UserUpdatedEvent") → "handleUserUpdatedEvent"
     */
    static generateHandlerMethodName(eventName: string): string {
        return `handle${eventName}`;
    }
}
