package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class OrderTrainDeletedEvent extends Event {
    private Integer trainAggregateId;

    public OrderTrainDeletedEvent() {
        super();
    }

    public OrderTrainDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderTrainDeletedEvent(Integer aggregateId, Integer trainAggregateId) {
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
