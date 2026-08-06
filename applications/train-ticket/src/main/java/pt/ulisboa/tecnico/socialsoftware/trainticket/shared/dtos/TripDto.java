package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;

public class TripDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private String tripType;
    private String tripNumber;
    private String startTime;
    private String endTime;
    private TripTrainDto trainType;
    private TripRouteDto route;
    private TripStartStationDto startStation;
    private TripTerminalStationDto terminalStation;

    public TripDto() {
    }

    public TripDto(Trip trip) {
        this.aggregateId = trip.getAggregateId();
        this.version = trip.getVersion();
        this.state = trip.getState();
        this.tripType = trip.getTripType() != null ? trip.getTripType().name() : null;
        this.tripNumber = trip.getTripNumber();
        this.startTime = trip.getStartTime();
        this.endTime = trip.getEndTime();
        this.trainType = trip.getTrainType() != null ? new TripTrainDto(trip.getTrainType()) : null;
        this.route = trip.getRoute() != null ? new TripRouteDto(trip.getRoute()) : null;
        this.startStation = trip.getStartStation() != null ? new TripStartStationDto(trip.getStartStation()) : null;
        this.terminalStation = trip.getTerminalStation() != null ? new TripTerminalStationDto(trip.getTerminalStation()) : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getTripType() {
        return tripType;
    }

    public void setTripType(String tripType) {
        this.tripType = tripType;
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public TripTrainDto getTrainType() {
        return trainType;
    }

    public void setTrainType(TripTrainDto trainType) {
        this.trainType = trainType;
    }

    public TripRouteDto getRoute() {
        return route;
    }

    public void setRoute(TripRouteDto route) {
        this.route = route;
    }

    public TripStartStationDto getStartStation() {
        return startStation;
    }

    public void setStartStation(TripStartStationDto startStation) {
        this.startStation = startStation;
    }

    public TripTerminalStationDto getTerminalStation() {
        return terminalStation;
    }

    public void setTerminalStation(TripTerminalStationDto terminalStation) {
        this.terminalStation = terminalStation;
    }
}