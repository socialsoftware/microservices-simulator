package pt.ulisboa.tecnico.socialsoftware.ms.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class EventHandler {
   private final JpaRepository<? extends Aggregate, Integer> aggregateRepository;

   protected EventHandler(JpaRepository<? extends Aggregate, Integer> aggregateRepository) {
      this.aggregateRepository = aggregateRepository;
   }

   public Set<Integer> getAggregateIds() {
      return aggregateRepository.findAll().stream()
              .map(Aggregate::getAggregateId)
              .collect(Collectors.toSet());
   }

   public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
      return aggregateRepository.findAll().stream()
              .filter(aggregate -> Objects.equals(aggregate.getAggregateId(), subscriberAggregateId))
              .filter(aggregate -> aggregate.getState() == Aggregate.AggregateState.ACTIVE)
              .max(Comparator.comparing(Aggregate::getVersion, Comparator.nullsLast(Long::compareTo)))
              .map(aggregate -> aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName()))
              .orElse(Collections.emptySet());
   }

   public abstract void handleEvent(Integer subscriberAggregateId, Event event);
}
