package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class PriceConfigTrainDeletedEvent extends Event {
    private Integer trainAggregateId;

    public PriceConfigTrainDeletedEvent() {
        super();
    }

    public PriceConfigTrainDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PriceConfigTrainDeletedEvent(Integer aggregateId, Integer trainAggregateId) {
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
