package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RouteDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private String startStationName;
    private String endStationName;
    private Set<RouteStationDto> routeStations = new HashSet<>();

    public RouteDto() {
    }

    public RouteDto(String startStationName, String endStationName, Set<RouteStationDto> routeStations) {
        this.startStationName = startStationName;
        this.endStationName = endStationName;
        setRouteStations(routeStations);
    }

    public RouteDto(Route route) {
        this.aggregateId = route.getAggregateId();
        this.version = route.getVersion();
        this.state = route.getState();
        this.startStationName = route.getStartStationName();
        this.endStationName = route.getEndStationName();
        setRouteStations(route.getRouteStations().stream()
                .map(RouteStationDto::new)
                .collect(Collectors.toSet()));
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

    public String getStartStationName() {
        return this.startStationName;
    }

    public void setStartStationName(String startStationName) {
        this.startStationName = startStationName;
    }

    public String getEndStationName() {
        return this.endStationName;
    }

    public void setEndStationName(String endStationName) {
        this.endStationName = endStationName;
    }

    public Set<RouteStationDto> getRouteStations() {
        return this.routeStations;
    }

    public void setRouteStations(Set<RouteStationDto> routeStations) {
        this.routeStations = routeStations == null ? new HashSet<>() : new HashSet<>(routeStations);
    }
}
