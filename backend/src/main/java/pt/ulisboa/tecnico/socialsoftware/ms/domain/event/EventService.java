package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateRepository;

@Service
public class EventService {
    @Autowired
    AggregateRepository aggregateRepository;
    @Autowired
    EventRepository eventRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
        Aggregate aggregate = aggregateRepository.findLastAggregateVersion(subscriberAggregateId).orElse(null);

        if (aggregate != null) {
            return aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName());
        } else {
            return new HashSet<>();
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<? extends Event> getSubscribedEvents(EventSubscription eventSubscription, Class<? extends Event> eventClass) {
        return eventRepository.findAll().stream()
                .filter(eventSubscription::subscribesEvent)
                .filter(eventClass::isInstance) 
                .map(eventClass::cast)
                .sorted(Comparator.comparing(Event::getTimestamp).reversed())
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void clearEventsAtApplicationStartUp() {
        eventRepository.deleteAll();
    }
}
