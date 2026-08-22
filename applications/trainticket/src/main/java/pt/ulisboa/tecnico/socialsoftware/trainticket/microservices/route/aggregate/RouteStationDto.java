package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

public class RouteStationDto {
    private Integer sequence;
    private Integer stationAggregateId;
    private String stationName;
    private Integer distanceFromStart;

    public RouteStationDto() {
    }

    public RouteStationDto(Integer sequence, Integer stationAggregateId, String stationName, Integer distanceFromStart) {
        this.sequence = sequence;
        this.stationAggregateId = stationAggregateId;
        this.stationName = stationName;
        this.distanceFromStart = distanceFromStart;
    }

    public RouteStationDto(RouteStation routeStation) {
        this.sequence = routeStation.getSequence();
        this.stationAggregateId = routeStation.getStationAggregateId();
        this.stationName = routeStation.getStationName();
        this.distanceFromStart = routeStation.getDistanceFromStart();
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
