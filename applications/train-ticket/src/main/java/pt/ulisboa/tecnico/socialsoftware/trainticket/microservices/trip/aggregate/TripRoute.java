package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripRouteDto;

@Entity
public class TripRoute {
    @Id
    @GeneratedValue
    private Long id;
    private Integer routeAggregateId;
    private Integer routeVersion;
    private AggregateState routeState;
    @OneToOne
    private Trip trip;

    public TripRoute() {

    }

    public TripRoute(RouteDto routeDto) {
        setRouteAggregateId(routeDto.getAggregateId());
        setRouteVersion(routeDto.getVersion());
        setRouteState(routeDto.getState());
    }

    public TripRoute(TripRouteDto tripRouteDto) {
        setRouteAggregateId(tripRouteDto.getAggregateId());
        setRouteVersion(tripRouteDto.getVersion());
        setRouteState(tripRouteDto.getState() != null ? AggregateState.valueOf(tripRouteDto.getState()) : null);
    }

    public TripRoute(TripRoute other) {
        setRouteAggregateId(other.getRouteAggregateId());
        setRouteVersion(other.getRouteVersion());
        setRouteState(other.getRouteState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRouteAggregateId() {
        return routeAggregateId;
    }

    public void setRouteAggregateId(Integer routeAggregateId) {
        this.routeAggregateId = routeAggregateId;
    }

    public Integer getRouteVersion() {
        return routeVersion;
    }

    public void setRouteVersion(Integer routeVersion) {
        this.routeVersion = routeVersion;
    }

    public AggregateState getRouteState() {
        return routeState;
    }

    public void setRouteState(AggregateState routeState) {
        this.routeState = routeState;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }




    public TripRouteDto buildDto() {
        TripRouteDto dto = new TripRouteDto();
        dto.setAggregateId(getRouteAggregateId());
        dto.setVersion(getRouteVersion());
        dto.setState(getRouteState() != null ? getRouteState().name() : null);
        return dto;
    }
}