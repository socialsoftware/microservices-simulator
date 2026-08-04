package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;

@Entity
public class RouteStation {
    @Id
    @GeneratedValue
    private Long id;
    private Integer stationOrder;
    private Integer distanceFromStart;
    private String stationName;
    private Integer stationAggregateId;
    private Integer stationVersion;
    private AggregateState stationState;
    @OneToOne
    private Route route;

    public RouteStation() {

    }

    public RouteStation(StationDto stationDto) {
        setStationAggregateId(stationDto.getAggregateId());
        setStationVersion(stationDto.getVersion());
        setStationState(stationDto.getState());
    }

    public RouteStation(RouteStationDto routeStationDto) {
        setStationOrder(routeStationDto.getStationOrder());
        setDistanceFromStart(routeStationDto.getDistanceFromStart());
        setStationName(routeStationDto.getName());
        setStationAggregateId(routeStationDto.getAggregateId());
        setStationVersion(routeStationDto.getVersion());
        setStationState(routeStationDto.getState() != null ? AggregateState.valueOf(routeStationDto.getState()) : null);
    }

    public RouteStation(RouteStation other) {
        setStationOrder(other.getStationOrder());
        setDistanceFromStart(other.getDistanceFromStart());
        setStationName(other.getStationName());
        setStationAggregateId(other.getStationAggregateId());
        setStationVersion(other.getStationVersion());
        setStationState(other.getStationState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public Integer getStationVersion() {
        return stationVersion;
    }

    public void setStationVersion(Integer stationVersion) {
        this.stationVersion = stationVersion;
    }

    public AggregateState getStationState() {
        return stationState;
    }

    public void setStationState(AggregateState stationState) {
        this.stationState = stationState;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }




    public RouteStationDto buildDto() {
        RouteStationDto dto = new RouteStationDto();
        dto.setStationOrder(getStationOrder());
        dto.setDistanceFromStart(getDistanceFromStart());
        dto.setName(getStationName());
        dto.setAggregateId(getStationAggregateId());
        dto.setVersion(getStationVersion());
        dto.setState(getStationState() != null ? getStationState().name() : null);
        return dto;
    }
}