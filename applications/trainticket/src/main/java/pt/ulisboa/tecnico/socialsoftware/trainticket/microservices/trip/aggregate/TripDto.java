package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;

import java.time.LocalTime;

public class TripDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private String tripNumber;
    private Integer routeAggregateId;
    private Integer trainTypeAggregateId;
    private LocalTime startTime;
    private LocalTime endTime;

    public TripDto() {
    }

    public TripDto(String tripNumber, Integer routeAggregateId, Integer trainTypeAggregateId,
                   LocalTime startTime, LocalTime endTime) {
        this.tripNumber = tripNumber;
        this.routeAggregateId = routeAggregateId;
        this.trainTypeAggregateId = trainTypeAggregateId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TripDto(Trip trip) {
        this.aggregateId = trip.getAggregateId();
        this.version = trip.getVersion();
        this.state = trip.getState();
        this.tripNumber = trip.getTripNumber();
        this.routeAggregateId = trip.getRouteAggregateId();
        this.trainTypeAggregateId = trip.getTrainTypeAggregateId();
        this.startTime = trip.getStartTime();
        this.endTime = trip.getEndTime();
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

    public String getTripNumber() {
        return this.tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public Integer getRouteAggregateId() {
        return this.routeAggregateId;
    }

    public void setRouteAggregateId(Integer routeAggregateId) {
        this.routeAggregateId = routeAggregateId;
    }

    public Integer getTrainTypeAggregateId() {
        return this.trainTypeAggregateId;
    }

    public void setTrainTypeAggregateId(Integer trainTypeAggregateId) {
        this.trainTypeAggregateId = trainTypeAggregateId;
    }

    public LocalTime getStartTime() {
        return this.startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return this.endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
