import { PublishedEvent, SubscribedEvent } from "../../../../../language/generated/ast.js";
import { AggregateExt } from "../../../../types/ast-extensions.js";
import { GeneratorCapabilities } from "../../../common/generator-capabilities.js";
import { PublishedEventContextBuilder } from "./published-event-context-builder.js";
import { SubscribedEventContextBuilder } from "./subscribed-event-context-builder.js";



export class EventContextBuilder {
    private publishedBuilder: PublishedEventContextBuilder;
    private subscribedBuilder: SubscribedEventContextBuilder;

    constructor(capabilities: GeneratorCapabilities) {
        this.publishedBuilder = new PublishedEventContextBuilder(capabilities);
        this.subscribedBuilder = new SubscribedEventContextBuilder(capabilities);
    }

    
    
    

    

    buildPublishedEventContext(event: PublishedEvent, aggregate: AggregateExt, options: { projectName: string }): any {
        return this.publishedBuilder.buildPublishedEventContext(event, aggregate, options);
    }

    

    buildSubscribedEventContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildSubscribedEventContext(event, aggregate, options);
    }

    

    buildInterInvariantSubscriptionContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildInterInvariantSubscriptionContext(event, aggregate, options);
    }

    

    buildCustomEventHandlingContext(aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildCustomEventHandlingContext(aggregate, options);
    }

    

    buildEventHandlerContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildEventHandlerContext(event, aggregate, options);
    }

    

    buildBaseEventHandlerContext(aggregate: AggregateExt, options: { projectName: string }): any {
        return this.subscribedBuilder.buildBaseEventHandlerContext(aggregate, options);
    }
}
