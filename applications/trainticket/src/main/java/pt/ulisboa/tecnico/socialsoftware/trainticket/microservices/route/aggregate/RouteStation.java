package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import jakarta.persistence.Column;
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
    // "sequence" is a reserved word in SQL, so the column carries a prefixed name.
    @Column(name = "station_sequence")
    private Integer sequence;
    @Column(name = "station_aggregate_id")
    private Integer stationAggregateId;
    @Column(name = "station_name")
    private String stationName;
    @Column(name = "distance_from_start")
    private Integer distanceFromStart;

    public RouteStation() {
    }

    public RouteStation(Integer sequence, Integer stationAggregateId, String stationName, Integer distanceFromStart) {
        setSequence(sequence);
        setStationAggregateId(stationAggregateId);
        setStationName(stationName);
        setDistanceFromStart(distanceFromStart);
    }

    public RouteStation(RouteStationDto routeStationDto) {
        setSequence(routeStationDto.getSequence());
        setStationAggregateId(routeStationDto.getStationAggregateId());
        setStationName(routeStationDto.getStationName());
        setDistanceFromStart(routeStationDto.getDistanceFromStart());
    }

    public RouteStation(RouteStation other) {
        setSequence(other.getSequence());
        setStationAggregateId(other.getStationAggregateId());
        setStationName(other.getStationName());
        setDistanceFromStart(other.getDistanceFromStart());
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSequence() {
        return this.sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getStationAggregateId() {
        return this.stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public String getStationName() {
        return this.stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Integer getDistanceFromStart() {
        return this.distanceFromStart;
    }

    public void setDistanceFromStart(Integer distanceFromStart) {
        this.distanceFromStart = distanceFromStart;
    }
}
