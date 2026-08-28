package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

/**
 * One event delivery captured by the oracle.
 * <p>
 * Identity contract: the persisted event ID identifies the event record; the
 * handler class and subscriber aggregate ID identify one delivery target. Event
 * ID alone is not a valid step ID because one event may fan out to several
 * subscribers or handler classes, and one delivery may be retried after later
 * progress. The step after which this invocation was captured is added when an
 * {@link EventHandlerStep} is created and identifies a retry attempt in the
 * current oracle model. Event class and publisher aggregate ID are retained as
 * diagnostic context, not as the sole source of uniqueness.
 * <p>
 * The event ID is captured when this value is created. This is required because
 * events are mutable JPA entities and mutable values must not be used in the
 * equality or hash code of an object stored in a set or map.
 * <p>
 * The integer ID is unique in the simulator's single {@code Event} table and is
 * safe within one oracle run. It is not a universal distributed event identity;
 * an oracle combining independent service databases must include the
 * originating service database in its identity or use a globally unique event
 * ID.
 */
final class DeferredEventInvocation {

    private final Event event;
    private final EventHandler handler;
    private final Integer subscriberAggregateId;
    private final Runnable invocation;
    private final Integer eventId;
    private final Integer publisherAggregateId;

    DeferredEventInvocation(
            Event event, EventHandler handler, Integer subscriberAggregateId, Runnable invocation) {

        this.event = Objects.requireNonNull(event, "Event cannot be null");
        this.handler = Objects.requireNonNull(handler, "EventHandler cannot be null");
        this.subscriberAggregateId = Objects.requireNonNull(subscriberAggregateId,
                "SubscriberAggregateId cannot be null");
        this.invocation = Objects.requireNonNull(invocation, "Invocation cannot be null");
        this.eventId = Objects.requireNonNull(event.getId(),
                "Deferred event invocation requires a non-null persisted event ID");
        this.publisherAggregateId = Objects.requireNonNull(event.getPublisherAggregateId(),
                "Deferred event invocation requires a non-null publisher aggregate ID");
    }

    Event event() {
        return event;
    }

    EventHandler handler() {
        return handler;
    }

    Integer subscriberAggregateId() {
        return subscriberAggregateId;
    }

    Runnable invocation() {
        return invocation;
    }

    Integer eventId() {
        return eventId;
    }

    Integer publisherAggregateId() {
        return publisherAggregateId;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;

        if (!(o instanceof DeferredEventInvocation other))
            return false;

        // Exclude the 'invocation' Runnable, as lambdas do not have stable equality.
        return eventId.equals(other.eventId)
                && handler.getClass().equals(other.handler.getClass())
                && subscriberAggregateId.equals(other.subscriberAggregateId);
    }

    @Override
    public int hashCode() {
        // Exclude the 'invocation' Runnable, as lambdas do not have stable hashing.
        return Objects.hash(eventId, handler.getClass(), subscriberAggregateId);
    }
}
