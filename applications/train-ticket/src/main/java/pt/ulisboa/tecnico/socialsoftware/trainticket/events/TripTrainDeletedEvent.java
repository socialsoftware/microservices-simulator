package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class TripTrainDeletedEvent extends Event {
    private Integer trainAggregateId;

    public TripTrainDeletedEvent() {
        super();
    }

    public TripTrainDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripTrainDeletedEvent(Integer aggregateId, Integer trainAggregateId) {
        super(aggregateId);
        setTrainAggregateId(trainAggregateId);
    }

    public Integer getTrainAggregateId() {
        return trainAggregateId;
    }

    public void setTrainAggregateId(Integer trainAggregateId) {
        this.trainAggregateId = trainAggregateId;
    }

}
