package pt.ulisboa.tecnico.socialsoftware.ms.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.IVersionService;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EventReplayCoordinatorTest {
    private EventReplayCoordinator.Activation activation;

    @AfterEach
    void cleanup() {
        if (activation != null) activation.close();
        System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY);
    }

    @Test
    void requiresPreStartupPropertyAndSuppressesUnscopedPolling() {
        assertThatThrownBy(EventReplayCoordinator::activate)
                .isInstanceOf(EventReplayException.class)
                .extracting(failure -> ((EventReplayException) failure).reason())
                .isEqualTo("EVENT_REPLAY_CONTROL_FAILED");

        activate();
        EventService eventService = mock(EventService.class);
        EventApplicationService applicationService = applicationService(eventService);
        EventHandler handler = mock(EventHandler.class);

        applicationService.handleSubscribedEvent(TestEvent.class, handler);

        verifyNoInteractions(eventService, handler);
        assertThat(EventReplayCoordinator.suppressUnscopedPolling()).isTrue();
    }

    @Test
    void capturesOnePersistedEventAndDispatchesOnlyThatEventSynchronously() {
        activate();
        TestEvent event = event(17, 7, 29L);
        List<EventReplayCoordinator.CapturedEvent> captured;
        try (EventReplayCoordinator.TriggerCaptureScope scope =
                     EventReplayCoordinator.beginTriggerCapture("trigger-1")) {
            EventReplayCoordinator.beforeEventRegistration();
            EventReplayCoordinator.recordPersistedEvent(event);
            captured = scope.capturedEvents();
        }
        assertThat(captured).containsExactly(new EventReplayCoordinator.CapturedEvent(
                17, TestEvent.class.getName(), 7, 29L, true));

        EventService eventService = mock(EventService.class);
        when(eventService.getEventForReplay(17)).thenReturn(event);
        EventHandler handler = mock(EventHandler.class);
        EventSubscription subscription = mock(EventSubscription.class);
        when(handler.getAggregateIds()).thenReturn(Set.of(41));
        when(handler.getEventSubscriptions(41, TestEvent.class)).thenReturn(Set.of(subscription));
        when(subscription.subscribesEvent(event)).thenReturn(true);
        EventApplicationService applicationService = applicationService(eventService);

        try (EventReplayCoordinator.SelectedEventScope selected =
                     EventReplayCoordinator.beginSelectedEvent(captured.get(0), TestEvent.class.getName(),
                             handler.getClass().getName())) {
            applicationService.handleSubscribedEvent(TestEvent.class, handler);
            selected.verifyCompleted();
            assertThat(selected.subscriberAggregateId()).isEqualTo(41);
        }

        verify(handler).handleEvent(41, event);
        assertThat(EventReplayCoordinator.currentSelectedEvent()).isEmpty();
        EventReplayCoordinator.assertNoOpenThreadScope();
    }

    @Test
    void enforcesSubscriberCardinalityAndAlwaysCleansSelectionScope() {
        activate();
        TestEvent event = event(18, 8, 30L);
        EventReplayCoordinator.CapturedEvent captured = new EventReplayCoordinator.CapturedEvent(
                18, TestEvent.class.getName(), 8, 30L, true);
        EventService eventService = mock(EventService.class);
        when(eventService.getEventForReplay(18)).thenReturn(event);
        EventHandler handler = mock(EventHandler.class);
        EventSubscription first = mock(EventSubscription.class);
        EventSubscription second = mock(EventSubscription.class);
        when(handler.getAggregateIds()).thenReturn(Set.of(1, 2));
        when(handler.getEventSubscriptions(1, TestEvent.class)).thenReturn(Set.of(first));
        when(handler.getEventSubscriptions(2, TestEvent.class)).thenReturn(Set.of(second));
        when(first.subscribesEvent(event)).thenReturn(true);
        when(second.subscribesEvent(event)).thenReturn(true);

        assertThatThrownBy(() -> {
            try (EventReplayCoordinator.SelectedEventScope ignored =
                         EventReplayCoordinator.beginSelectedEvent(captured, TestEvent.class.getName(),
                                 handler.getClass().getName())) {
                applicationService(eventService).handleSubscribedEvent(TestEvent.class, handler);
            }
        }).isInstanceOf(EventReplayException.class)
                .extracting(failure -> ((EventReplayException) failure).reason())
                .isEqualTo("MULTIPLE_MATCHING_SUBSCRIBERS_UNSUPPORTED");
        assertThat(EventReplayCoordinator.currentSelectedEvent()).isEmpty();
    }

    @Test
    void sagaRegistrationCapturesPersistedIdentityAndRejectsRecursiveRegistrationBeforeSaving() {
        activate();
        SagaUnitOfWorkService unitOfWorkService = new SagaUnitOfWorkService();
        IVersionService versionService = mock(IVersionService.class);
        when(versionService.incrementAndGetVersionNumber()).thenReturn(44L);
        CapturingEventService eventService = new CapturingEventService();
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        ReflectionTestUtils.setField(unitOfWorkService, "versionService", versionService);
        ReflectionTestUtils.setField(unitOfWorkService, "eventService", eventService);
        ReflectionTestUtils.setField(unitOfWorkService, "environment", environment);
        TestEvent event = event(null, 12, 0L);

        List<EventReplayCoordinator.CapturedEvent> captured;
        try (EventReplayCoordinator.TriggerCaptureScope scope =
                     EventReplayCoordinator.beginTriggerCapture("trigger-register")) {
            unitOfWorkService.registerEvent(event, new SagaUnitOfWork(1L, "fixture"));
            captured = scope.capturedEvents();
        }
        assertThat(captured).containsExactly(new EventReplayCoordinator.CapturedEvent(
                1, TestEvent.class.getName(), 12, 44L, true));

        EventReplayCoordinator.CapturedEvent selectedEvent = captured.get(0);
        assertThatThrownBy(() -> {
            try (EventReplayCoordinator.SelectedEventScope ignored =
                         EventReplayCoordinator.beginSelectedEvent(selectedEvent, TestEvent.class.getName(), "handler")) {
                unitOfWorkService.registerEvent(new TestEvent(12), new SagaUnitOfWork(2L, "nested"));
            }
        }).isInstanceOf(EventReplayException.class)
                .extracting(failure -> ((EventReplayException) failure).reason())
                .isEqualTo("RECURSIVE_EVENT_CONSEQUENCE_UNSUPPORTED");
        assertThat(eventService.saved).hasSize(1);
    }

    @Test
    void rejectsRecursiveRegistrationDuringSelectedDispatch() {
        activate();
        EventReplayCoordinator.CapturedEvent captured = new EventReplayCoordinator.CapturedEvent(
                19, TestEvent.class.getName(), 9, 31L, true);

        assertThatThrownBy(() -> {
            try (EventReplayCoordinator.SelectedEventScope ignored =
                         EventReplayCoordinator.beginSelectedEvent(captured, TestEvent.class.getName(),
                                 "handler")) {
                EventReplayCoordinator.beforeEventRegistration();
            }
        }).isInstanceOf(EventReplayException.class)
                .extracting(failure -> ((EventReplayException) failure).reason())
                .isEqualTo("RECURSIVE_EVENT_CONSEQUENCE_UNSUPPORTED");
        EventReplayCoordinator.assertNoOpenThreadScope();
    }

    private void activate() {
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY, "true");
        activation = EventReplayCoordinator.activate();
    }

    private EventApplicationService applicationService(EventService eventService) {
        EventApplicationService service = new EventApplicationService();
        ReflectionTestUtils.setField(service, "eventService", eventService);
        return service;
    }

    private TestEvent event(Integer id, int publisherId, long version) {
        TestEvent event = new TestEvent(publisherId);
        event.setId(id);
        event.setPublisherAggregateVersion(version);
        event.setPublished(true);
        return event;
    }

    static class CapturingEventService extends EventService {
        final List<Event> saved = new java.util.ArrayList<>();

        @Override
        public void saveEvent(Event event) {
            event.setId(saved.size() + 1);
            saved.add(event);
        }
    }

    static class TestEvent extends Event {
        TestEvent(Integer publisherAggregateId) {
            super(publisherAggregateId);
        }
    }
}
