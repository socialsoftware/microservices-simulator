package pt.ulisboa.tecnico.socialsoftware.ms.aggregate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayException;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserverHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactWriterContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.PersistentStateObserver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class EventApplicationService {
    @Autowired
    private EventService eventService;
    @Autowired
    private PersistentStateObserver persistentStateObserver;

    // It is not transactional to allow the concurrent execution of events
    public void handleSubscribedEvent(Class<? extends Event> eventClass, EventHandler eventHandler) {
        if (EventReplayCoordinator.isActive()) {
            EventReplayCoordinator.SelectedEventScope selection = EventReplayCoordinator.currentSelectedEvent()
                    .orElse(null);
            if (selection == null) {
                return;
            }
            handleSelectedEvent(eventClass, eventHandler, selection);
            return;
        }
        Set<Integer> aggregateIds = eventHandler.getAggregateIds();
        for (Integer subscriberAggregateId : aggregateIds) {
            Set<EventSubscription> eventSubscriptions = eventHandler.getEventSubscriptions(subscriberAggregateId, eventClass);

            for (EventSubscription eventSubscription: eventSubscriptions) {
                List<? extends Event> eventsToProcess = eventService.getSubscribedEvents(eventSubscription, eventClass);
                for (Event eventToProcess : eventsToProcess) {
                    eventHandler.handleEvent(subscriberAggregateId, eventToProcess);
                }
            }
        }
    }

    private void handleSelectedEvent(Class<? extends Event> eventClass,
                                     EventHandler eventHandler,
                                     EventReplayCoordinator.SelectedEventScope selection) {
        if (!Objects.equals(eventClass.getName(), selection.expectedEventTypeFqn())
                || !Objects.equals(eventHandler.getClass().getName(), selection.expectedHandlerClassFqn())) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                    "selected event type or handler does not match the persisted route");
        }
        Event event = eventService.getEventForReplay(selection.event().eventId());
        if (!Objects.equals(event.getClass().getName(), selection.event().eventTypeFqn())
                || !Objects.equals(event.getClass().getName(), selection.expectedEventTypeFqn())
                || !Objects.equals(event.getPublisherAggregateId(), selection.event().publisherAggregateId())
                || !Objects.equals(event.getPublisherAggregateVersion(), selection.event().publisherAggregateVersion())
                || event.isPublished() != selection.event().published()) {
            throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                    "persisted event no longer matches captured runtime identity");
        }

        List<Integer> eligibleSubscribers = new ArrayList<>();
        for (Integer aggregateId : eventHandler.getAggregateIds().stream().sorted(Comparator.naturalOrder()).toList()) {
            boolean eligible = eventHandler.getEventSubscriptions(aggregateId, eventClass).stream()
                    .anyMatch(subscription -> subscription.subscribesEvent(event));
            if (eligible) {
                eligibleSubscribers.add(aggregateId);
            }
        }
        if (eligibleSubscribers.isEmpty()) {
            selection.recordNoEligibleSubscriber();
            return;
        }
        if (eligibleSubscribers.size() != 1) {
            throw new EventReplayException("MULTIPLE_MATCHING_SUBSCRIBERS_UNSUPPORTED",
                    "selected event has " + eligibleSubscribers.size() + " eligible subscribers");
        }
        Integer subscriberAggregateId = eligibleSubscribers.get(0);
        ReceiverObservation before = observeReceiver(subscriberAggregateId, event, "BEFORE_EVENT");
        ImpactEvidence.Writer parent = ImpactWriterContext.current().orElse(ImpactEvidence.Writer.unknown(null, null));
        ImpactEvidence.Writer eventWriter = new ImpactEvidence.Writer(
                "EVENT_CONSUMER", parent.executionAttemptId(), parent.workloadPlanId(), parent.sagaInstanceId(),
                parent.actionId(), "EVENT", parent.functionalityName(), parent.stepName(), event.getId());
        try (ImpactWriterContext.Scope ignored = ImpactWriterContext.enter(eventWriter)) {
            eventHandler.handleEvent(subscriberAggregateId, event);
        }
        selection.recordDelivery(subscriberAggregateId);
        observeDelivery(event, subscriberAggregateId, before, eventWriter);
    }

    private ReceiverObservation observeReceiver(Integer aggregateId, Event event, String stage) {
        if (!ImpactEvidenceObserverHolder.isEnabled()) return new ReceiverObservation(null, false);
        try {
            PersistentStateObserver.EligibilityObservation observation =
                    persistentStateObserver.observeEligibility(aggregateId, event, stage);
            observation.projection().gaps().forEach(ImpactEvidenceObserverHolder::coverageGap);
            return new ReceiverObservation(observation.projection().snapshot(), observation.eligible());
        } catch (RuntimeException failure) {
            ImpactEvidenceObserverHolder.coverageGap(new ImpactEvidence.CoverageGap(
                    stage, String.valueOf(aggregateId), "RECEIVER_PROJECTION_FAILED",
                    failure.getClass().getName() + ": " + failure.getMessage()));
            return new ReceiverObservation(null, false);
        }
    }

    private void observeDelivery(Event event,
                                 Integer subscriberAggregateId,
                                 ReceiverObservation before,
                                 ImpactEvidence.Writer writer) {
        if (!ImpactEvidenceObserverHolder.isEnabled()) return;
        try {
            PersistentStateObserver.EligibilityObservation observation = before.snapshot() == null
                    ? persistentStateObserver.observeEligibility(subscriberAggregateId, event, "AFTER_EVENT")
                    : persistentStateObserver.observeEligibility(before.snapshot().identity(), event, "AFTER_EVENT");
            observation.projection().gaps().forEach(ImpactEvidenceObserverHolder::coverageGap);
            ImpactEvidence.AggregateSnapshot after = observation.projection().snapshot();
            ImpactEvidenceObserverHolder.eventDelivery(new ImpactEvidence.EventDelivery(
                    0L, event.getId(), event.getClass().getName(), event.getPublisherAggregateId(),
                    event.getPublisherAggregateVersion(), before.snapshot(), after, before.eligible(),
                    observation.eligible(),
                    null, null, writer));
        } catch (RuntimeException failure) {
            ImpactEvidenceObserverHolder.coverageGap(new ImpactEvidence.CoverageGap(
                    "AFTER_EVENT", String.valueOf(subscriberAggregateId), "EVENT_OBSERVATION_FAILED",
                    failure.getClass().getName() + ": " + failure.getMessage()));
        }
    }

    private record ReceiverObservation(ImpactEvidence.AggregateSnapshot snapshot, boolean eligible) {
    }
}
