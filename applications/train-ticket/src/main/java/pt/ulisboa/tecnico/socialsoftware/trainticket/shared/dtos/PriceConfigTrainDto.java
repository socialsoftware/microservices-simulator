package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigTrain;

public class PriceConfigTrainDto implements Serializable {
    private String name;
    private Integer aggregateId;
    private Long version;
    private String state;

    public PriceConfigTrainDto() {
    }

    public PriceConfigTrainDto(PriceConfigTrain priceConfigTrain) {
        this.name = priceConfigTrain.getTrainTypeName();
        this.aggregateId = priceConfigTrain.getTrainAggregateId();
        this.version = priceConfigTrain.getTrainVersion();
        this.state = priceConfigTrain.getTrainState() != null ? priceConfigTrain.getTrainState().name() : null;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}