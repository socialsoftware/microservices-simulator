package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventApplicationService {
    @Autowired
    private EventService eventService;

    // It is not transactional to allow the concurrent execution of events
    public void handleSubscribedEvent(Class<? extends Event> eventClass, EventHandler eventHandler) {
        Set<Integer> aggregateIds = eventHandler.getAggregateIds();
        for (Integer subscriberAggregateId : aggregateIds) {
            Set<EventSubscription> eventSubscriptions = eventService.getEventSubscriptions(subscriberAggregateId, eventClass);

            for (EventSubscription eventSubscription: eventSubscriptions) {
                List<? extends Event> eventsToProcess = eventService.getSubscribedEvents(eventSubscription, eventClass);
                for (Event eventToProcess : eventsToProcess) {
                    eventHandler.handleEvent(subscriberAggregateId, eventToProcess);
                }
            }
        }
    }
}
