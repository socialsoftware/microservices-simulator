package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;

public class PriceConfigDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private Double basicPriceRate;
    private Double firstClassPriceRate;
    private PriceConfigTrainDto trainType;
    private PriceConfigRouteDto route;

    public PriceConfigDto() {
    }

    public PriceConfigDto(PriceConfig priceConfig) {
        this.aggregateId = priceConfig.getAggregateId();
        this.version = priceConfig.getVersion();
        this.state = priceConfig.getState();
        this.basicPriceRate = priceConfig.getBasicPriceRate();
        this.firstClassPriceRate = priceConfig.getFirstClassPriceRate();
        this.trainType = priceConfig.getTrainType() != null ? new PriceConfigTrainDto(priceConfig.getTrainType()) : null;
        this.route = priceConfig.getRoute() != null ? new PriceConfigRouteDto(priceConfig.getRoute()) : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Double getBasicPriceRate() {
        return basicPriceRate;
    }

    public void setBasicPriceRate(Double basicPriceRate) {
        this.basicPriceRate = basicPriceRate;
    }

    public Double getFirstClassPriceRate() {
        return firstClassPriceRate;
    }

    public void setFirstClassPriceRate(Double firstClassPriceRate) {
        this.firstClassPriceRate = firstClassPriceRate;
    }

    public PriceConfigTrainDto getTrainType() {
        return trainType;
    }

    public void setTrainType(PriceConfigTrainDto trainType) {
        this.trainType = trainType;
    }

    public PriceConfigRouteDto getRoute() {
        return route;
    }

    public void setRoute(PriceConfigRouteDto route) {
        this.route = route;
    }
}