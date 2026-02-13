import { PublishedEvent, SubscribedEvent } from "../../../../../language/generated/ast.js";
import { AggregateExt } from "../../../../types/ast-extensions.js";
import { GeneratorCapabilities } from "../../../common/generator-capabilities.js";
import { PublishedEventContextBuilder } from "./published-event-context-builder.js";
import { SubscribedEventContextBuilder } from "./subscribed-event-context-builder.js";

/**
 * Event Context Builder (Facade)
 *
 * Orchestrates event context building by delegating to specialized builders.
 * This class serves as a backward-compatible facade after refactoring.
 *
 * Responsibilities:
 * - Maintain existing public API
 * - Delegate to specialized builders:
 *   - PublishedEventContextBuilder: Published events
 *   - SubscribedEventContextBuilder: Subscribed events, inter-invariants, handlers
 *   - EventPublisherDiscovery: Publisher aggregate lookup
 */
export class EventContextBuilder {
    private publishedBuilder: PublishedEventContextBuilder;
    private subscribedBuilder: SubscribedEventContextBuilder;

    constructor(capabilities: GeneratorCapabilities) {
        this.publishedBuilder = new PublishedEventContextBuilder(capabilities);
        this.subscribedBuilder = new SubscribedEventContextBuilder(capabilities);
    }

    // ============================================================================
    // PUBLIC CONTEXT BUILDING METHODS
    // ============================================================================

    /**
     * Build context for a published event.
     * Delegates to PublishedEventContextBuilder.
     */
    buildPublishedEventContext(event: PublishedEvent, aggregate: AggregateExt, options: { projectName: string }): any {
        return this.publishedBuilder.buildPublishedEventContext(event, aggregate, options);
    }

    /**
     * Build context for a subscribed event.
     * Delegates to SubscribedEventContextBuilder.
     */
    buildSubscribedEventContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildSubscribedEventContext(event, aggregate, options);
    }

    /**
     * Build context for an inter-invariant subscription.
     * Delegates to SubscribedEventContextBuilder.
     */
    buildInterInvariantSubscriptionContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildInterInvariantSubscriptionContext(event, aggregate, options);
    }

    /**
     * Build context for custom event handling (main event routing class).
     * Delegates to SubscribedEventContextBuilder.
     */
    buildCustomEventHandlingContext(aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildCustomEventHandlingContext(aggregate, options);
    }

    /**
     * Build context for a specific event handler.
     * Delegates to SubscribedEventContextBuilder.
     */
    buildEventHandlerContext(event: SubscribedEvent, aggregate: AggregateExt, options: any): any {
        return this.subscribedBuilder.buildEventHandlerContext(event, aggregate, options);
    }

    /**
     * Build context for base event handler class.
     * Delegates to SubscribedEventContextBuilder.
     */
    buildBaseEventHandlerContext(aggregate: AggregateExt, options: { projectName: string }): any {
        return this.subscribedBuilder.buildBaseEventHandlerContext(aggregate, options);
    }
}
