package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRoute;

public class PriceConfigRouteDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private String state;

    public PriceConfigRouteDto() {
    }

    public PriceConfigRouteDto(PriceConfigRoute priceConfigRoute) {
        this.aggregateId = priceConfigRoute.getRouteAggregateId();
        this.version = priceConfigRoute.getRouteVersion();
        this.state = priceConfigRoute.getRouteState() != null ? priceConfigRoute.getRouteState().name() : null;
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