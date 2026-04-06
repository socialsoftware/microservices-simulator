




export type EventAction = 'create' | 'update' | 'delete' | 'unknown';



export interface ParsedEventName {
    

    fullName: string;
    

    baseName: string;
    

    entityName: string;
    

    action: EventAction;
    

    publisherAggregate: string;
}



export class EventNameParser {
    

    static removeEventSuffix(eventName: string): string {
        return eventName.replace(/Event$/, '');
    }

    

    static extractEntityName(eventName: string): string {
        
        let result = eventName.replace(/^(Update|Delete|Create)/, '');

        
        result = result.replace(/(Updated|Deleted|Created)Event$/, '');

        
        if (result === eventName) {
            result = eventName.replace(/Event$/, '');
        }

        return result;
    }

    

    static extractAction(eventName: string): EventAction {
        if (eventName.match(/Created?Event$/)) return 'create';
        if (eventName.match(/Updated?Event$/)) return 'update';
        if (eventName.match(/Deleted?Event$/)) return 'delete';
        return 'unknown';
    }

    

    static extractPublisherAggregate(eventName: string): string {
        
        return eventName.replace(/(Deleted|Updated|Created)?Event$/, '');
    }

    

    static isCrudEvent(eventName: string): boolean {
        return eventName.match(/(Created|Updated|Deleted)Event$/) !== null;
    }

    

    static isUpdateEvent(eventName: string): boolean {
        return eventName.match(/Updated?Event$/) !== null;
    }

    

    static isDeleteEvent(eventName: string): boolean {
        return eventName.match(/Deleted?Event$/) !== null;
    }

    

    static isCreateEvent(eventName: string): boolean {
        return eventName.match(/Created?Event$/) !== null;
    }

    

    static parse(eventName: string): ParsedEventName {
        return {
            fullName: eventName,
            baseName: this.removeEventSuffix(eventName),
            entityName: this.extractEntityName(eventName),
            action: this.extractAction(eventName),
            publisherAggregate: this.extractPublisherAggregate(eventName)
        };
    }

    

    static generateSubscriptionName(aggregateName: string, eventName: string): string {
        const eventWithoutSuffix = this.removeEventSuffix(eventName);
        return `${aggregateName}Subscribes${eventWithoutSuffix}`;
    }

    

    static generateHandlerMethodName(eventName: string): string {
        return `handle${eventName}`;
    }
}
