package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class TrainDeletedEvent extends Event {

    public TrainDeletedEvent() {
        super();
    }

    public TrainDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
