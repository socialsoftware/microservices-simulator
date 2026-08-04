package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;

public class RouteDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private List<RouteStationDto> stations;

    public RouteDto() {
    }

    public RouteDto(Route route) {
        this.aggregateId = route.getAggregateId();
        this.version = route.getVersion();
        this.state = route.getState();
        this.stations = route.getStations() != null ? route.getStations().stream().map(RouteStation::buildDto).collect(Collectors.toList()) : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public List<RouteStationDto> getStations() {
        return stations;
    }

    public void setStations(List<RouteStationDto> stations) {
        this.stations = stations;
    }
}