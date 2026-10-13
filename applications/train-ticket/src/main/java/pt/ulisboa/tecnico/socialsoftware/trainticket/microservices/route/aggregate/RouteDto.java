package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RouteDto {
    private Integer aggregateId;
    private Long version;
    private List<RouteStationDto> stations = new ArrayList<>();

    public RouteDto() {
    }

    public RouteDto(Integer aggregateId, Long version, List<RouteStationDto> stations) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.stations = stations;
    }

    public RouteDto(Route route) {
        this.aggregateId = route.getAggregateId();
        this.version = route.getVersion();
        this.stations = route.getOrderedStations().stream()
                .map(RouteStationDto::new)
                .collect(Collectors.toList());
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

    public List<RouteStationDto> getStations() {
        return stations;
    }

    public void setStations(List<RouteStationDto> stations) {
        this.stations = stations;
    }
}
