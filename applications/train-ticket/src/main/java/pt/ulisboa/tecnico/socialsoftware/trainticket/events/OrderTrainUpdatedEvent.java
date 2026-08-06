package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class OrderTrainUpdatedEvent extends Event {
    private Integer trainAggregateId;
    private Long trainVersion;
    private String trainNumber;

    public OrderTrainUpdatedEvent() {
        super();
    }

    public OrderTrainUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderTrainUpdatedEvent(Integer aggregateId, Integer trainAggregateId, Long trainVersion, String trainNumber) {
        super(aggregateId);
        setTrainAggregateId(trainAggregateId);
        setTrainVersion(trainVersion);
        setTrainNumber(trainNumber);
    }

    public Integer getTrainAggregateId() {
        return trainAggregateId;
    }

    public void setTrainAggregateId(Integer trainAggregateId) {
        this.trainAggregateId = trainAggregateId;
    }

    public Long getTrainVersion() {
        return trainVersion;
    }

    public void setTrainVersion(Long trainVersion) {
        this.trainVersion = trainVersion;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

}
