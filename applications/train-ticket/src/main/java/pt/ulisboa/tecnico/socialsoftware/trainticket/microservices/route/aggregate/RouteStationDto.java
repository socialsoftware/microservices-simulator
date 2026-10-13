package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

public class RouteStationDto {
    private Integer stationAggregateId;
    private String stationName;
    private Long stationVersion;
    private Integer sequence;
    private Integer distanceFromStart;

    public RouteStationDto() {
    }

    public RouteStationDto(Integer stationAggregateId, String stationName, Long stationVersion, Integer sequence,
                           Integer distanceFromStart) {
        this.stationAggregateId = stationAggregateId;
        this.stationName = stationName;
        this.stationVersion = stationVersion;
        this.sequence = sequence;
        this.distanceFromStart = distanceFromStart;
    }

    public RouteStationDto(RouteStation routeStation) {
        this.stationAggregateId = routeStation.getStationAggregateId();
        this.stationName = routeStation.getStationName();
        this.stationVersion = routeStation.getStationVersion();
        this.sequence = routeStation.getSequence();
        this.distanceFromStart = routeStation.getDistanceFromStart();
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
