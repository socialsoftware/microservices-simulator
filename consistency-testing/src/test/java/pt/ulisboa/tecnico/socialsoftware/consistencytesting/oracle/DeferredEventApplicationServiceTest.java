package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static DeferredEventInvocation onlyInvocation(Set<DeferredEventInvocation> invocations) {
        assertEquals(1, invocations.size());
        return invocations.iterator().next();
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
