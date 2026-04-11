package pt.ulisboa.tecnico.socialsoftware.ms.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {
    @Autowired
    EventRepository eventRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<? extends Event> getSubscribedEvents(EventSubscription eventSubscription, Class<? extends Event> eventClass) {
        return eventRepository.findUnprocessedEvents(
                eventClass,
                eventSubscription.getSubscribedAggregateId(),
                eventSubscription.getSubscribedVersion()
        ).stream().map(eventClass::cast).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void clearEventsAtApplicationStartUp() {
        eventRepository.deleteAll();
    }
}
