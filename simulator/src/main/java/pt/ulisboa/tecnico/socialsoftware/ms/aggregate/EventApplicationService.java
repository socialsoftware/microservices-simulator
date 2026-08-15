package pt.ulisboa.tecnico.socialsoftware.ms.aggregate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayException;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class EventApplicationService {
    @Autowired
    private EventService eventService;

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
            throw new EventReplayException("SELECTED_SUBSCRIBER_NOT_FOUND",
                    "selected event has no eligible subscriber for the persisted route");
        }
        if (eligibleSubscribers.size() != 1) {
            throw new EventReplayException("MULTIPLE_MATCHING_SUBSCRIBERS_UNSUPPORTED",
                    "selected event has " + eligibleSubscribers.size() + " eligible subscribers");
        }
        Integer subscriberAggregateId = eligibleSubscribers.get(0);
        eventHandler.handleEvent(subscriberAggregateId, event);
        selection.recordDelivery(subscriberAggregateId);
    }
}
