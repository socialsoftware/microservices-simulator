package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe.TripSubscribesRouteDeletedTripRouteExists;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe.TripSubscribesStationDeletedTripStationsExist;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe.TripSubscribesTrainDeletedTripTraintypeExists;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripRouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripStartStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripTerminalStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripTrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.TripType;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Trip extends Aggregate {
    @Enumerated(EnumType.STRING)
    private TripType tripType;
    private String tripNumber;
    private String startTime;
    private String endTime;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "trip")
    private TripTrain trainType;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "trip")
    private TripRoute route;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "trip")
    private TripStartStation startStation;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "trip")
    private TripTerminalStation terminalStation;

    public Trip() {

    }

    public Trip(Integer aggregateId, TripDto tripDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTripType(TripType.valueOf(tripDto.getTripType()));
        setTripNumber(tripDto.getTripNumber());
        setStartTime(tripDto.getStartTime());
        setEndTime(tripDto.getEndTime());
        setTrainType(tripDto.getTrainType() != null ? new TripTrain(tripDto.getTrainType()) : null);
        setRoute(tripDto.getRoute() != null ? new TripRoute(tripDto.getRoute()) : null);
        setStartStation(tripDto.getStartStation() != null ? new TripStartStation(tripDto.getStartStation()) : null);
        setTerminalStation(tripDto.getTerminalStation() != null ? new TripTerminalStation(tripDto.getTerminalStation()) : null);
    }


    public Trip(Trip other) {
        super(other);
        setTripType(other.getTripType());
        setTripNumber(other.getTripNumber());
        setStartTime(other.getStartTime());
        setEndTime(other.getEndTime());
        setTrainType(new TripTrain(other.getTrainType()));
        setRoute(new TripRoute(other.getRoute()));
        setStartStation(new TripStartStation(other.getStartStation()));
        setTerminalStation(new TripTerminalStation(other.getTerminalStation()));
    }

    public TripType getTripType() {
        return tripType;
    }

    public void setTripType(TripType tripType) {
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

    public TripTrain getTrainType() {
        return trainType;
    }

    public void setTrainType(TripTrain trainType) {
        this.trainType = trainType;
        if (this.trainType != null) {
            this.trainType.setTrip(this);
        }
    }

    public TripRoute getRoute() {
        return route;
    }

    public void setRoute(TripRoute route) {
        this.route = route;
        if (this.route != null) {
            this.route.setTrip(this);
        }
    }

    public TripStartStation getStartStation() {
        return startStation;
    }

    public void setStartStation(TripStartStation startStation) {
        this.startStation = startStation;
        if (this.startStation != null) {
            this.startStation.setTrip(this);
        }
    }

    public TripTerminalStation getTerminalStation() {
        return terminalStation;
    }

    public void setTerminalStation(TripTerminalStation terminalStation) {
        this.terminalStation = terminalStation;
        if (this.terminalStation != null) {
            this.terminalStation.setTrip(this);
        }
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantTripTraintypeExists(eventSubscriptions);
            interInvariantTripRouteExists(eventSubscriptions);
            interInvariantTripStationsExist(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantTripTraintypeExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TripSubscribesTrainDeletedTripTraintypeExists(this.getTrainType()));
    }

    private void interInvariantTripRouteExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TripSubscribesRouteDeletedTripRouteExists(this.getRoute()));
    }

    private void interInvariantTripStationsExist(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TripSubscribesStationDeletedTripStationsExist(this.getStartStation()));
    }


    private boolean invariantTripTypeSet() {
        return this.tripType != null;
    }

    private boolean invariantTripNumberNotBlank() {
        return this.tripNumber != null && this.tripNumber != null && this.tripNumber.length() > 0;
    }

    private boolean invariantTrainTypeSet() {
        return this.trainType != null;
    }

    private boolean invariantRouteSet() {
        return this.route != null;
    }

    private boolean invariantStartStationSet() {
        return this.startStation != null;
    }

    private boolean invariantTerminalStationSet() {
        return this.terminalStation != null;
    }

    private boolean invariantStartTimeSet() {
        return this.startTime != null && this.startTime != null && this.startTime.length() > 0;
    }

    private boolean invariantEndTimeSet() {
        return this.endTime != null && this.endTime != null && this.endTime.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantTripTypeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have a trip type");
        }
        if (!invariantTripNumberNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have a trip number");
        }
        if (!invariantTrainTypeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have a train type");
        }
        if (!invariantRouteSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have a route");
        }
        if (!invariantStartStationSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have a start station");
        }
        if (!invariantTerminalStationSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have a terminal station");
        }
        if (!invariantStartTimeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have a start time");
        }
        if (!invariantEndTimeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Trip must have an end time");
        }
    }

    public TripDto buildDto() {
        TripDto dto = new TripDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setTripType(getTripType() != null ? getTripType().name() : null);
        dto.setTripNumber(getTripNumber());
        dto.setStartTime(getStartTime());
        dto.setEndTime(getEndTime());
        dto.setTrainType(getTrainType() != null ? new TripTrainDto(getTrainType()) : null);
        dto.setRoute(getRoute() != null ? new TripRouteDto(getRoute()) : null);
        dto.setStartStation(getStartStation() != null ? new TripStartStationDto(getStartStation()) : null);
        dto.setTerminalStation(getTerminalStation() != null ? new TripTerminalStationDto(getTerminalStation()) : null);
        return dto;
    }
}