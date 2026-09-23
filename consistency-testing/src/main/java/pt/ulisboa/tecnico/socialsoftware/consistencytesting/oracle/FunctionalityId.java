package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    /**
     * Identity with database-generated delivery identifiers removed where possible.
     */
    private final String behavioralIdentity;

    private FunctionalityId(String id) {
        this(id, id);
    }

    private FunctionalityId(String id, String behavioralIdentity) {
        this.id = id;
        this.behavioralIdentity = behavioralIdentity;
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

        String concreteIdentity = String.join(ID_CONNECTOR,
                "event", eventClazz.getName(),
                "eventId", encodeEventId(eventId),
                "fromAggregate", publisherAggregateId.toString(),
                "toAggregate", subscriberAggregateId.toString(),
                "capturedAfter", boundedCapturedAfterIdentity(
                        capturedAfterStepId, capturedAfterStepId.toString()),
                "withHandler", eventHandlerClazz.getName());

        /*
         * Persisted event and aggregate IDs are recreated between runs. Keep them in
         * the concrete scheduler identity, but omit them from the observational
         * identity used to compare behavior across runs. Captured-after remains: it
         * distinguishes deliveries materialized at different logical points, and is
         * already normalized recursively when it is itself an event delivery.
         */
        String behavioralIdentity = String.join(ID_CONNECTOR,
                "event", eventClazz.getName(),
                "capturedAfter", boundedCapturedAfterIdentity(
                        capturedAfterStepId, capturedAfterStepId.behavioralIdentity()),
                "withHandler", eventHandlerClazz.getName());

        return new FunctionalityId(concreteIdentity, behavioralIdentity);
    }

    /**
     * Returns identity used for behavioral comparison, never for scheduling or
     * addressing a concrete event delivery.
     */
    String behavioralIdentity() {
        return behavioralIdentity;
    }

    /**
     * Pads event ID with leading zeroes so lexical StepId ordering also orders
     * event IDs numerically; for example, {@code 2} sorts before {@code 10}.
     */
    private static String encodeEventId(Integer eventId) {
        return String.format(Locale.ROOT, "%010d", eventId);
    }

    /**
     * Direct source steps stay readable. For an event caused or retried after another
     * event delivery, retain collision-resistant ancestry without recursively
     * embedding the complete parent identity. This bounds IDs even for long event
     * chains while preserving distinct concrete retry attempts.
     */
    private static String boundedCapturedAfterIdentity(StepId capturedAfterStepId, String identity) {
        if (capturedAfterStepId.stepKind() != StepKind.EVENT_HANDLER) {
            return identity;
        }
        return "eventHandlerStepHash-" + sha256(identity);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to bound event-handler identities", e);
        }
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
