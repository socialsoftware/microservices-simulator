package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "route_stations")
public class RouteStation {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer stationAggregateId;
    private String stationName;
    private Long stationVersion;
    private Integer sequence;
    private Integer distanceFromStart;

    public RouteStation() {
    }

    public RouteStation(Integer stationAggregateId, String stationName, Long stationVersion, Integer sequence,
                        Integer distanceFromStart) {
        this.stationAggregateId = stationAggregateId;
        this.stationName = stationName;
        this.stationVersion = stationVersion;
        this.sequence = sequence;
        this.distanceFromStart = distanceFromStart;
    }

    public RouteStation(RouteStationDto routeStationDto) {
        this.stationAggregateId = routeStationDto.getStationAggregateId();
        this.stationName = routeStationDto.getStationName();
        this.stationVersion = routeStationDto.getStationVersion();
        this.sequence = routeStationDto.getSequence();
        this.distanceFromStart = routeStationDto.getDistanceFromStart();
    }

    public RouteStation(RouteStation other) {
        this.stationAggregateId = other.getStationAggregateId();
        this.stationName = other.getStationName();
        this.stationVersion = other.getStationVersion();
        this.sequence = other.getSequence();
        this.distanceFromStart = other.getDistanceFromStart();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Long getStationVersion() {
        return stationVersion;
    }

    public void setStationVersion(Long stationVersion) {
        this.stationVersion = stationVersion;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getDistanceFromStart() {
        return distanceFromStart;
    }

    public void setDistanceFromStart(Integer distanceFromStart) {
        this.distanceFromStart = distanceFromStart;
    }
}
