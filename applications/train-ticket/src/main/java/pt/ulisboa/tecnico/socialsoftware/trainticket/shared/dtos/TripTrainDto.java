package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripTrain;

public class TripTrainDto implements Serializable {
    private String name;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public TripTrainDto() {
    }

    public TripTrainDto(TripTrain tripTrain) {
        this.name = tripTrain.getTrainTypeName();
        this.aggregateId = tripTrain.getTrainAggregateId();
        this.version = tripTrain.getTrainVersion();
        this.state = tripTrain.getTrainState() != null ? tripTrain.getTrainState().name() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}