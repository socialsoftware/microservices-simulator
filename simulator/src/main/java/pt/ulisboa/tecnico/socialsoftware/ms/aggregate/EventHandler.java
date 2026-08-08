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

   // AggregateRepository is declared over Aggregate, so findAll() returns every aggregate of every
   // type. Without this filter a handler is handed the ids of foreign aggregates that subscribe to
   // the same event type, and the consumer's own service casts them to its aggregate class.
   public Set<Integer> getAggregateIds() {
      return aggregateRepository.findAll().stream()
              .filter(aggregate -> aggregateType().isInstance(aggregate))
              .map(Aggregate::getAggregateId)
              .collect(Collectors.toSet());
   }

   public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
      return aggregateRepository.findAll().stream()
              .filter(aggregate -> aggregateType().isInstance(aggregate))
              .filter(aggregate -> Objects.equals(aggregate.getAggregateId(), subscriberAggregateId))
              .filter(aggregate -> aggregate.getState() == Aggregate.AggregateState.ACTIVE)
              .max(Comparator.comparing(Aggregate::getVersion, Comparator.nullsLast(Long::compareTo)))
              .map(aggregate -> aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName()))
              .orElse(Collections.emptySet());
   }

   protected abstract Class<? extends Aggregate> aggregateType();

   public abstract void handleEvent(Integer subscriberAggregateId, Event event);
}
