package pt.ulisboa.tecnico.socialsoftware.ms.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.util.List;

@Service
public class EventService {
    @Autowired
    EventRepository eventRepository;

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    public boolean existsEventById(Integer eventId) {
        return eventRepository.existsById(eventId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<? extends Event> getSubscribedEvents(EventSubscription eventSubscription, Class<? extends Event> eventClass) {
        return eventRepository.findUnprocessedEvents(
                eventClass,
                eventSubscription.getSubscribedAggregateId(),
                eventSubscription.getSubscribedVersion()
        ).stream().map(eventClass::cast).toList();
    }

    public void clearEventsAtApplicationStartUp() {
        eventRepository.deleteAll();
    }
}
