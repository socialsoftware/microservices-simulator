package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

record DeferredEventInvocation(
                Event event,
                EventHandler handler,
                Integer subscriberAggregateId,
                Runnable invocation) {

        public Integer publisherAggregateId() {
                return Objects.requireNonNull(event.getPublisherAggregateId());
        }

        // ! TODO is it correct to use event.getId() instead of instance equality?
        // ! TODO is it correct to use handler.class instead of instance equality?

        @Override
        public boolean equals(@Nullable Object o) {
                if (this == o)
                        return true;

                if (!(o instanceof DeferredEventInvocation other))
                        return false;

                // Exclude the 'invocation' Runnable, as lambdas do not have stable equality
                return Objects.equals(subscriberAggregateId, other.subscriberAggregateId)
                                && Objects.equals(event.getId(), other.event.getId())
                                && Objects.equals(handler.getClass(), other.handler.getClass());
        }

        @Override
        public int hashCode() {
                // Exclude the 'invocation' Runnable, as lambdas do not have stable hashing
                return Objects.hash(event.getId(), handler.getClass(), subscriberAggregateId);
        }
}
