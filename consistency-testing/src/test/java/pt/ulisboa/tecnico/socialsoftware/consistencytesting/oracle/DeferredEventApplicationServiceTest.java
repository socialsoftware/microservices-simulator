package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

class DeferredEventApplicationServiceTest {

    @Test
    void collapsesDuplicateDispatchesFromOnePollingPass() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingEventHandler handler = new CountingEventHandler();
        TestEvent event = new TestEvent();
        event.setId(10);
        event.setPublisherAggregateId(20);

        try (DeferredEventApplicationService.CaptureSession session = service.beginCapture()) {
            service.dispatchToHandler(handler, 30, event);
            service.dispatchToHandler(handler, 30, event);

            Set<DeferredEventInvocation> invocations = session.drain();
            assertEquals(event, onlyInvocation(invocations).event());
        }
    }

    @Test
    void capturesPendingEventAgainWhenItIsPolledAfterAnUnsuccessfulAttempt() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingEventHandler handler = new CountingEventHandler();
        TestEvent event = new TestEvent();
        event.setId(10);
        event.setPublisherAggregateId(20);

        try (DeferredEventApplicationService.CaptureSession session = service.beginCapture()) {
            service.dispatchToHandler(handler, 30, event);
            DeferredEventInvocation firstAttempt = onlyInvocation(session.drain());

            firstAttempt.invocation().run();
            assertEquals(1, handler.invocationCount);

            service.dispatchToHandler(handler, 30, event);

            assertEquals(event, onlyInvocation(session.drain()).event(),
                    "A later poll must recapture an event whose subscription did not advance");
        }
    }

    @Test
    void snapshotsEventIdWhenCaptureIsCreated() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingEventHandler handler = new CountingEventHandler();
        TestEvent event = event(10);

        try (DeferredEventApplicationService.CaptureSession session = service.beginCapture()) {
            service.dispatchToHandler(handler, 30, event);
            DeferredEventInvocation invocation = onlyInvocation(session.drain());
            Set<DeferredEventInvocation> invocations = new HashSet<>();
            invocations.add(invocation);

            event.setId(11);

            assertEquals(10, invocation.eventId(),
                    "DeferredEventInvocation must snapshot the event ID at the time of capture so that it cannot be affected by later changes to the event object");
            assertTrue(invocations.contains(invocation));
            assertTrue(new EventHandlerStep(invocation, StepId.forInitialStateSetupStep())
                    .getId().toString().contains("eventId-0000000010"));
        }
    }

    @Test
    void rejectsDeferredCaptureWithoutPersistedEventId() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingEventHandler handler = new CountingEventHandler();
        TestEvent event = event(null);

        try (DeferredEventApplicationService.CaptureSession session = service.beginCapture()) {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> service.dispatchToHandler(handler, 30, event));

            assertEquals(
                    "Deferred event invocation requires a non-null persisted event ID",
                    exception.getMessage());
        }
    }

    private static DeferredEventInvocation onlyInvocation(Set<DeferredEventInvocation> invocations) {
        assertEquals(1, invocations.size());
        return invocations.iterator().next();
    }

    private static TestEvent event(Integer id) {
        TestEvent event = new TestEvent();
        event.setId(id);
        event.setPublisherAggregateId(20);
        return event;
    }

    private static final class TestEvent extends Event {
    }

    private static final class CountingEventHandler extends EventHandler {
        private int invocationCount;

        private CountingEventHandler() {
            super(null);
        }

        @Override
        public void handleEvent(Integer subscriberAggregateId, Event event) {
            invocationCount++;
        }
    }
}
