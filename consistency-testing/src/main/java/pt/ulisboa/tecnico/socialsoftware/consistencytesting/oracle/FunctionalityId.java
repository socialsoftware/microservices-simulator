package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

public final class FunctionalityId {
    private static final String ID_CONNECTOR = "-";

    /**
     * Singleton of the synthetic functionality that represents the entire initial
     * state setup.
     */
    private static final FunctionalityId INITIAL_STATE_SETUP_FUNCTIONALITY = new FunctionalityId(
            "initialStateSetup");

    private final String id;

    private FunctionalityId(String id) {
        this.id = id;
    }

    public static FunctionalityId forSagaFunctionality(String functionalityId) {
        return new FunctionalityId(functionalityId);
    }

    /**
     * The synthetic functionality that represents the entire initial state setup.
     */
    public static FunctionalityId forInitialStateSetupFunctionality() {
        return INITIAL_STATE_SETUP_FUNCTIONALITY;
    }

    static FunctionalityId forEventHandlerFunctionality(
            DeferredEventInvocation invocation, StepId capturedAfterStepId) {

        Objects.requireNonNull(invocation, "Deferred event invocation cannot be null");
        return createEventHandlerFunctionalityId(
                invocation.eventId(),
                invocation.event().getClass(),
                invocation.handler().getClass(),
                capturedAfterStepId,
                invocation.subscriberAggregateId(),
                invocation.publisherAggregateId());
    }

    private static FunctionalityId createEventHandlerFunctionalityId(
            Integer eventId,
            Class<? extends Event> eventClazz,
            Class<? extends EventHandler> eventHandlerClazz,
            StepId capturedAfterStepId,
            Integer subscriberAggregateId,
            Integer publisherAggregateId) {

        return new FunctionalityId(String.join(ID_CONNECTOR,
                "event", eventClazz.getName(),
                "eventId", encodeEventId(eventId),
                "fromAggregate", publisherAggregateId.toString(),
                "toAggregate", subscriberAggregateId.toString(),
                "capturedAfter", capturedAfterStepId.toString(),
                "withHandler", eventHandlerClazz.getName()));
    }

    /**
     * Pads event ID with leading zeroes so lexical StepId ordering also orders
     * event IDs numerically; for example, {@code 2} sorts before {@code 10}.
     */
    private static String encodeEventId(Integer eventId) {
        return String.format(Locale.ROOT, "%010d", eventId);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionalityId other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
