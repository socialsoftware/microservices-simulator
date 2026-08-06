package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class TripTrainUpdatedEvent extends Event {
    private Integer trainAggregateId;
    private Long trainVersion;
    private String trainTypeName;

    public TripTrainUpdatedEvent() {
        super();
    }

    public TripTrainUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripTrainUpdatedEvent(Integer aggregateId, Integer trainAggregateId, Long trainVersion, String trainTypeName) {
        super(aggregateId);
        setTrainAggregateId(trainAggregateId);
        setTrainVersion(trainVersion);
        setTrainTypeName(trainTypeName);
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

    public String getTrainTypeName() {
        return trainTypeName;
    }

    public void setTrainTypeName(String trainTypeName) {
        this.trainTypeName = trainTypeName;
    }

}
