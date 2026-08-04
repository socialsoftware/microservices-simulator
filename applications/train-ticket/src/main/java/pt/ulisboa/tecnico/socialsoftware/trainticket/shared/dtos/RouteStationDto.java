package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;

public class RouteStationDto implements Serializable {
    private Integer stationOrder;
    private Integer distanceFromStart;
    private String name;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public RouteStationDto() {
    }

    public RouteStationDto(RouteStation routeStation) {
        this.stationOrder = routeStation.getStationOrder();
        this.distanceFromStart = routeStation.getDistanceFromStart();
        this.name = routeStation.getStationName();
        this.aggregateId = routeStation.getStationAggregateId();
        this.version = routeStation.getStationVersion();
        this.state = routeStation.getStationState() != null ? routeStation.getStationState().name() : null;
    }

    public Integer getStationOrder() {
        return stationOrder;
    }

    public void setStationOrder(Integer stationOrder) {
        this.stationOrder = stationOrder;
    }

    public Integer getDistanceFromStart() {
        return distanceFromStart;
    }

    public void setDistanceFromStart(Integer distanceFromStart) {
        this.distanceFromStart = distanceFromStart;
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