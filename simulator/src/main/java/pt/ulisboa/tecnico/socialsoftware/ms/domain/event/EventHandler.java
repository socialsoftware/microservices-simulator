package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import java.util.Set;

public abstract class EventHandler {
   public abstract Set<Integer> getAggregateIds();
   public abstract void handleEvent(Integer subscriberAggregateId, Event event);
}
