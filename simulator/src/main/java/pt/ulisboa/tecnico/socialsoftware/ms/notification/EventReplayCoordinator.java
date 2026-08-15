package pt.ulisboa.tecnico.socialsoftware.ms.notification;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EventReplayCoordinator {
    public static final String REPLAY_MODE_PROPERTY = "microservices.simulator.event-replay.enabled";

    private static volatile Activation active;
    private static final ThreadLocal<TriggerCaptureScope> currentCapture = new ThreadLocal<>();
    private static final ThreadLocal<SelectedEventScope> currentSelection = new ThreadLocal<>();

    private EventReplayCoordinator() {
    }

    public static synchronized Activation activate() {
        if (!Boolean.parseBoolean(System.getProperty(REPLAY_MODE_PROPERTY, "false"))) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                    "event replay mode property must be active before application startup");
        }
        if (active != null) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED", "event replay mode is already active");
        }
        active = new Activation();
        return active;
    }

    public static boolean isActive() {
        return active != null;
    }

    public static TriggerCaptureScope beginTriggerCapture(String triggerScheduledStepId) {
        requireActive();
        if (currentCapture.get() != null || currentSelection.get() != null) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED", "event replay scope is already active");
        }
        TriggerCaptureScope scope = new TriggerCaptureScope(triggerScheduledStepId);
        currentCapture.set(scope);
        return scope;
    }

    public static SelectedEventScope beginSelectedEvent(CapturedEvent event,
                                                         String expectedEventTypeFqn,
                                                         String expectedHandlerClassFqn) {
        requireActive();
        if (FaultVectorProviderHolder.currentBoundary().isPresent()) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                    "event consequence cannot run inside a fault-vector boundary");
        }
        if (currentCapture.get() != null || currentSelection.get() != null) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED", "event replay scope is already active");
        }
        if (event == null || event.eventId() == null) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED", "selected event has no persisted runtime id");
        }
        SelectedEventScope scope = new SelectedEventScope(event, expectedEventTypeFqn, expectedHandlerClassFqn);
        currentSelection.set(scope);
        return scope;
    }

    public static Optional<SelectedEventScope> currentSelectedEvent() {
        return Optional.ofNullable(currentSelection.get());
    }

    public static boolean suppressUnscopedPolling() {
        return isActive() && currentSelection.get() == null;
    }

    public static void beforeEventRegistration() {
        if (isActive() && currentSelection.get() != null) {
            throw new EventReplayException("RECURSIVE_EVENT_CONSEQUENCE_UNSUPPORTED",
                    "selected event consequence registered another event");
        }
    }

    public static void recordPersistedEvent(Event event) {
        if (!isActive()) {
            return;
        }
        TriggerCaptureScope capture = currentCapture.get();
        if (capture != null) {
            capture.record(event);
        }
    }

    public static void assertNoOpenThreadScope() {
        if (currentCapture.get() != null || currentSelection.get() != null
                || FaultVectorProviderHolder.currentBoundary().isPresent()) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED", "replay or fault scope leaked");
        }
    }

    private static void requireActive() {
        if (!isActive()) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED", "event replay mode is not active");
        }
    }

    private static synchronized void deactivate(Activation activation) {
        if (active == activation) {
            active = null;
        }
        currentCapture.remove();
        currentSelection.remove();
        FaultVectorProviderHolder.clear();
    }

    public record CapturedEvent(Integer eventId,
                                String eventTypeFqn,
                                Integer publisherAggregateId,
                                Long publisherAggregateVersion,
                                boolean published) {
    }

    public static final class Activation implements AutoCloseable {
        private boolean closed;

        private Activation() {
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                deactivate(this);
            }
        }
    }

    public static final class TriggerCaptureScope implements AutoCloseable {
        private final String triggerScheduledStepId;
        private final List<CapturedEvent> events = new ArrayList<>();
        private boolean closed;

        private TriggerCaptureScope(String triggerScheduledStepId) {
            if (triggerScheduledStepId == null || triggerScheduledStepId.isBlank()) {
                throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED", "trigger scheduled-step id is required");
            }
            this.triggerScheduledStepId = triggerScheduledStepId;
        }

        private void record(Event event) {
            if (event == null || event.getId() == null) {
                throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                        "captured event was not assigned a persisted runtime id");
            }
            events.add(new CapturedEvent(event.getId(), event.getClass().getName(),
                    event.getPublisherAggregateId(), event.getPublisherAggregateVersion(), event.isPublished()));
        }

        public String triggerScheduledStepId() {
            return triggerScheduledStepId;
        }

        public List<CapturedEvent> capturedEvents() {
            return List.copyOf(events);
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                if (currentCapture.get() == this) {
                    currentCapture.remove();
                }
            }
        }
    }

    public static final class SelectedEventScope implements AutoCloseable {
        private final CapturedEvent event;
        private final String expectedEventTypeFqn;
        private final String expectedHandlerClassFqn;
        private Integer subscriberAggregateId;
        private int deliveries;
        private boolean closed;

        private SelectedEventScope(CapturedEvent event,
                                   String expectedEventTypeFqn,
                                   String expectedHandlerClassFqn) {
            this.event = event;
            this.expectedEventTypeFqn = expectedEventTypeFqn;
            this.expectedHandlerClassFqn = expectedHandlerClassFqn;
        }

        public CapturedEvent event() {
            return event;
        }

        public String expectedEventTypeFqn() {
            return expectedEventTypeFqn;
        }

        public String expectedHandlerClassFqn() {
            return expectedHandlerClassFqn;
        }

        public void recordDelivery(Integer subscriberAggregateId) {
            deliveries++;
            if (deliveries != 1) {
                throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                        "selected event was dispatched more than once");
            }
            this.subscriberAggregateId = subscriberAggregateId;
        }

        public Integer subscriberAggregateId() {
            return subscriberAggregateId;
        }

        public void verifyCompleted() {
            if (deliveries != 1) {
                throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                        "selected event handling method did not dispatch exactly once");
            }
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                if (currentSelection.get() == this) {
                    currentSelection.remove();
                }
            }
        }
    }
}
