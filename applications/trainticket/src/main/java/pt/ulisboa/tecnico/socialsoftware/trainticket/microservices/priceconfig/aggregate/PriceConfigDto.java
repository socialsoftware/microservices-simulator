package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;

import java.math.BigDecimal;

public class PriceConfigDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private Integer routeAggregateId;
    private Integer trainTypeAggregateId;
    private BigDecimal basicPriceRate;
    private BigDecimal firstClassPriceRate;

    public PriceConfigDto() {
    }

    public PriceConfigDto(Integer routeAggregateId, Integer trainTypeAggregateId,
                          BigDecimal basicPriceRate, BigDecimal firstClassPriceRate) {
        this.routeAggregateId = routeAggregateId;
        this.trainTypeAggregateId = trainTypeAggregateId;
        this.basicPriceRate = basicPriceRate;
        this.firstClassPriceRate = firstClassPriceRate;
    }

    public PriceConfigDto(PriceConfig priceConfig) {
        this.aggregateId = priceConfig.getAggregateId();
        this.version = priceConfig.getVersion();
        this.state = priceConfig.getState();
        this.routeAggregateId = priceConfig.getRouteAggregateId();
        this.trainTypeAggregateId = priceConfig.getTrainTypeAggregateId();
        this.basicPriceRate = priceConfig.getBasicPriceRate();
        this.firstClassPriceRate = priceConfig.getFirstClassPriceRate();
    }

    public boolean isActive() {
        return this.state == Aggregate.AggregateState.ACTIVE;
    }

    public Integer getAggregateId() {
        return this.aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Aggregate.AggregateState getState() {
        return this.state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public Integer getRouteAggregateId() {
        return this.routeAggregateId;
    }

    public void setRouteAggregateId(Integer routeAggregateId) {
        this.routeAggregateId = routeAggregateId;
    }

    public Integer getTrainTypeAggregateId() {
        return this.trainTypeAggregateId;
    }

    public void setTrainTypeAggregateId(Integer trainTypeAggregateId) {
        this.trainTypeAggregateId = trainTypeAggregateId;
    }

    public BigDecimal getBasicPriceRate() {
        return this.basicPriceRate;
    }

    public void setBasicPriceRate(BigDecimal basicPriceRate) {
        this.basicPriceRate = basicPriceRate;
    }

    public BigDecimal getFirstClassPriceRate() {
        return this.firstClassPriceRate;
    }

    public void setFirstClassPriceRate(BigDecimal firstClassPriceRate) {
        this.firstClassPriceRate = firstClassPriceRate;
    }
}
