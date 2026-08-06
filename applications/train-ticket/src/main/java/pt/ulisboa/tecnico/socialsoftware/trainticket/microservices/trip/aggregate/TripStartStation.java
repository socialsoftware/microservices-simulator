package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripStartStationDto;

@Entity
public class TripStartStation {
    @Id
    @GeneratedValue
    private Long id;
    private String startStationName;
    private Integer stationAggregateId;
    private Long stationVersion;
    private AggregateState stationState;
    @OneToOne
    private Trip trip;

    public TripStartStation() {

    }

    public TripStartStation(StationDto stationDto) {
        setStationAggregateId(stationDto.getAggregateId());
        setStationVersion(stationDto.getVersion());
        setStationState(stationDto.getState());
    }

    public TripStartStation(TripStartStationDto tripStartStationDto) {
        setStartStationName(tripStartStationDto.getName());
        setStationAggregateId(tripStartStationDto.getAggregateId());
        setStationVersion(tripStartStationDto.getVersion());
        setStationState(tripStartStationDto.getState() != null ? AggregateState.valueOf(tripStartStationDto.getState()) : null);
    }

    public TripStartStation(TripStartStation other) {
        setStartStationName(other.getStartStationName());
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

    public String getStartStationName() {
        return startStationName;
    }

    public void setStartStationName(String startStationName) {
        this.startStationName = startStationName;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public Long getStationVersion() {
        return stationVersion;
    }

    public void setStationVersion(Long stationVersion) {
        this.stationVersion = stationVersion;
    }

    public AggregateState getStationState() {
        return stationState;
    }

    public void setStationState(AggregateState stationState) {
        this.stationState = stationState;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }




    public TripStartStationDto buildDto() {
        TripStartStationDto dto = new TripStartStationDto();
        dto.setName(getStartStationName());
        dto.setAggregateId(getStationAggregateId());
        dto.setVersion(getStationVersion());
        dto.setState(getStationState() != null ? getStationState().name() : null);
        return dto;
    }
}