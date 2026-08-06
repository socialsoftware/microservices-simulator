package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRoute;

public class TripRouteDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private String state;

    public TripRouteDto() {
    }

    public TripRouteDto(TripRoute tripRoute) {
        this.aggregateId = tripRoute.getRouteAggregateId();
        this.version = tripRoute.getRouteVersion();
        this.state = tripRoute.getRouteState() != null ? tripRoute.getRouteState().name() : null;
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