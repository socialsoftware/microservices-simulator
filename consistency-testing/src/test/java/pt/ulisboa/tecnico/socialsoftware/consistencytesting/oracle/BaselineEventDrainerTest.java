package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;

class BaselineEventDrainerTest {

    @Test
    void executesSetupDeliveriesInStableOrderThenProvesQuiescence() {
        DeferredEventApplicationService eventService = new DeferredEventApplicationService();
        RecordingHandler handler = new RecordingHandler();
        OneShotPolling polling = new OneShotPolling(eventService, handler, List.of(event(20), event(10)), 30);

        BaselineEventDrainer.drainToQuiescence(Set.of(polling), eventService);

        assertEquals(List.of(10, 20), handler.deliveredEventIds);
        assertEquals(2, polling.pollCount, "final empty poll is baseline-quiescence assertion");
    }

    @Test
    void failsFastWhenSetupNeverReachesQuiescence() {
        DeferredEventApplicationService eventService = new DeferredEventApplicationService();
        RecordingHandler handler = new RecordingHandler();
        RepeatingPolling polling = new RepeatingPolling(eventService, handler, 30);
        int maxDeliveries = 10;

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> BaselineEventDrainer.drainToQuiescence(Set.of(polling), eventService, maxDeliveries));

        assertEquals(IntStream.rangeClosed(1, maxDeliveries).boxed().toList(), handler.deliveredEventIds);
        assertEquals(("Catalog setup did not reach event-delivery quiescence after %d handled delivery(ies) "
                + "(limit=%d); 1 delivery(ies) remain pending: [eventId=%d, handler=%s, subscriberAggregateId=30]")
                .formatted(maxDeliveries, maxDeliveries, maxDeliveries + 1, RecordingHandler.class.getName()),
                error.getMessage());
    }

    private static TestEvent event(int id) {
        TestEvent event = new TestEvent();
        event.setId(id);
        event.setPublisherAggregateId(20);
        return event;
    }

    private static final class TestEvent extends Event {
    }

    private static final class RecordingHandler extends EventHandler {
        private final List<Integer> deliveredEventIds = new ArrayList<>();

        private RecordingHandler() {
            super(null);
        }

        @Override
        public void handleEvent(Integer subscriberAggregateId, Event event) {
            deliveredEventIds.add(event.getId());
        }
    }

    private static final class OneShotPolling implements EventHandling {
        private final DeferredEventApplicationService eventService;
        private final EventHandler handler;
        private final List<? extends Event> events;
        private final Integer subscriberAggregateId;
        private int pollCount;

        private OneShotPolling(
                DeferredEventApplicationService eventService,
                EventHandler handler,
                List<? extends Event> events,
                Integer subscriberAggregateId) {
            this.eventService = eventService;
            this.handler = handler;
            this.events = events;
            this.subscriberAggregateId = subscriberAggregateId;
        }

        @Scheduled(fixedDelay = 1000)
        public void poll() {
            pollCount++;
            if (pollCount == 1) {
                events.forEach(event -> eventService.dispatchToHandler(handler, subscriberAggregateId, event));
            }
        }
    }

    private static final class RepeatingPolling implements EventHandling {
        private final DeferredEventApplicationService eventService;
        private final EventHandler handler;
        private final Integer subscriberAggregateId;
        private int nextEventId = 1;

        private RepeatingPolling(
                DeferredEventApplicationService eventService,
                EventHandler handler,
                Integer subscriberAggregateId) {
            this.eventService = eventService;
            this.handler = handler;
            this.subscriberAggregateId = subscriberAggregateId;
        }

        @Scheduled(fixedDelay = 1000)
        public void poll() {
            eventService.dispatchToHandler(handler, subscriberAggregateId, event(nextEventId++));
        }
    }
}
