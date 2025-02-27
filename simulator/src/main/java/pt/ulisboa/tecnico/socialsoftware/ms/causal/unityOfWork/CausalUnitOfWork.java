package pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.CANNOT_PERFORM_CAUSAL_READ;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

@Profile("tcc")
public class CausalUnitOfWork extends UnitOfWork {

    private Map<Integer, Aggregate> causalSnapshot;

    public CausalUnitOfWork(Integer version, String functionalityName) {
        super(version, functionalityName);
        this.causalSnapshot = new HashMap<>();
    }

    public void addToCausalSnapshot(Aggregate aggregate, List<Event> allEvents) {
        verifyProcessedEventsByAggregate(aggregate, allEvents);
        verifyEmittedEventsByAggregate(aggregate, allEvents);
        verifySameProcessedEvents(aggregate, allEvents);
        addAggregateToSnapshot(aggregate);
    }

    private void verifyProcessedEventsByAggregate(Aggregate aggregate, List<Event> allEvents) {
        for (EventSubscription es : aggregate.getEventSubscriptions()) {
            for (Aggregate snapshotAggregate : this.causalSnapshot.values()) {
                List<Event> snapshotAggregateEmittedEvents = allEvents.stream()
                        .filter(e -> e.getPublisherAggregateId().equals(snapshotAggregate.getAggregateId()))
                        .filter(e -> e.getClass().getSimpleName().equals(es.getEventType()))
                        .filter(e -> e.getPublisherAggregateVersion() <= snapshotAggregate.getVersion())
                        .filter(e -> e.getPublisherAggregateVersion() > es.getSubscribedVersion())
                        .collect(Collectors.toList());
                // snapshotAggregateEmittedEvents is a list of emitted events of the same type of the current sub emitted
                // by the current snapshot aggregate emitted after the version of the current subscription

                // if there are events in those situations we verify whether they are relevant or not for the subscription
                for (Event snapshotAggregateEmittedEvent : snapshotAggregateEmittedEvents) {
                    if (es.subscribesEvent(snapshotAggregateEmittedEvent)) {
                        throw new TutorException(CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED, aggregate.getClass().getSimpleName(), snapshotAggregateEmittedEvent.getClass().getSimpleName());
                    }
                }
            }
        }
    }

    private void verifyEmittedEventsByAggregate(Aggregate aggregate, List<Event> allEvents) {
        for (Aggregate snapshotAggregate : this.causalSnapshot.values()) {
            for (EventSubscription es : snapshotAggregate.getEventSubscriptions()) {
                List<Event> aggregateEmittedEvents = allEvents.stream()
                        .filter(e -> e.getPublisherAggregateId().equals(snapshotAggregate.getAggregateId()))
                        .filter(e -> e.getClass().getSimpleName().equals(es.getEventType()))
                        .filter(e -> e.getPublisherAggregateVersion() <= snapshotAggregate.getVersion())
                        .filter(e -> e.getPublisherAggregateVersion() > es.getSubscribedVersion())
                        .collect(Collectors.toList());
                for (Event snapshotAggregateEmittedEvent : aggregateEmittedEvents) {
                    if (es.subscribesEvent(snapshotAggregateEmittedEvent)) {
                        throw new TutorException(CANNOT_PERFORM_CAUSAL_READ, aggregate.getAggregateId(), getVersion());
                    }
                }
            }
        }
    }

    private void verifySameProcessedEvents(Aggregate aggregate, List<Event> allEvents) {
        Set<EventSubscription> aggregateEventSubscriptions = aggregate.getEventSubscriptions();
        for(Aggregate snapshotAggregate : this.causalSnapshot.values()) {
            for(EventSubscription es1 : aggregateEventSubscriptions) {
                for (EventSubscription es2 : snapshotAggregate.getEventSubscriptions()) {
                    // if they correspond to the same aggregate and type
                    if (es1.getSubscribedAggregateId().equals(es2.getSubscribedAggregateId()) && es1.getEventType().equals(es2.getEventType())) {
                        Integer minVersion = Math.min(es1.getSubscribedVersion(), es2.getSubscribedVersion());
                        Integer maxVersion = Math.max(es1.getSubscribedVersion(), es2.getSubscribedVersion());
                        List<Event> eventsBetweenAggregates = allEvents.stream()
                                .filter(event -> event.getPublisherAggregateId().equals(es1.getSubscribedAggregateId()))
                                .filter(event -> event.getClass().getSimpleName().equals(es1.getEventType()))
                                .filter(event -> minVersion < event.getPublisherAggregateVersion() && event.getPublisherAggregateVersion() <= maxVersion)
                                .toList();
                        for (Event eventBetweenAggregates : eventsBetweenAggregates) {
                            if(es1.subscribesEvent(eventBetweenAggregates) && es2.subscribesEvent(eventBetweenAggregates)) {
                                throw new TutorException(CANNOT_PERFORM_CAUSAL_READ, aggregate.getAggregateId(), getVersion());
                            }
                        }
                    }
                }
            }
        }
    }

    private void addAggregateToSnapshot(Aggregate aggregate) {
        if(!this.causalSnapshot.containsKey(aggregate.getAggregateId()) || aggregate.getVersion() > this.causalSnapshot.get(aggregate.getAggregateId()).getVersion()) {
            this.causalSnapshot.put(aggregate.getAggregateId(), aggregate);
        }
    }
}
