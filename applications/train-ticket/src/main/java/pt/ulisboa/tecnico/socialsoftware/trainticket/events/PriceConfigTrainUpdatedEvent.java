package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class PriceConfigTrainUpdatedEvent extends Event {
    private Integer trainAggregateId;
    private Integer trainVersion;
    private String trainTypeName;

    public PriceConfigTrainUpdatedEvent() {
        super();
    }

    public PriceConfigTrainUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PriceConfigTrainUpdatedEvent(Integer aggregateId, Integer trainAggregateId, Integer trainVersion, String trainTypeName) {
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

    public Integer getTrainVersion() {
        return trainVersion;
    }

    public void setTrainVersion(Integer trainVersion) {
        this.trainVersion = trainVersion;
    }

    public String getTrainTypeName() {
        return trainTypeName;
    }

    public void setTrainTypeName(String trainTypeName) {
        this.trainTypeName = trainTypeName;
    }

}
