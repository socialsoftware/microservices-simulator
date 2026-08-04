package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripTerminalStationDto;

@Entity
public class TripTerminalStation {
    @Id
    @GeneratedValue
    private Long id;
    private String terminalStationName;
    private Integer stationAggregateId;
    private Integer stationVersion;
    private AggregateState stationState;
    @OneToOne
    private Trip trip;

    public TripTerminalStation() {

    }

    public TripTerminalStation(StationDto stationDto) {
        setStationAggregateId(stationDto.getAggregateId());
        setStationVersion(stationDto.getVersion());
        setStationState(stationDto.getState());
    }

    public TripTerminalStation(TripTerminalStationDto tripTerminalStationDto) {
        setTerminalStationName(tripTerminalStationDto.getName());
        setStationAggregateId(tripTerminalStationDto.getAggregateId());
        setStationVersion(tripTerminalStationDto.getVersion());
        setStationState(tripTerminalStationDto.getState() != null ? AggregateState.valueOf(tripTerminalStationDto.getState()) : null);
    }

    public TripTerminalStation(TripTerminalStation other) {
        setTerminalStationName(other.getTerminalStationName());
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

    public String getTerminalStationName() {
        return terminalStationName;
    }

    public void setTerminalStationName(String terminalStationName) {
        this.terminalStationName = terminalStationName;
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

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }




    public TripTerminalStationDto buildDto() {
        TripTerminalStationDto dto = new TripTerminalStationDto();
        dto.setName(getTerminalStationName());
        dto.setAggregateId(getStationAggregateId());
        dto.setVersion(getStationVersion());
        dto.setState(getStationState() != null ? getStationState().name() : null);
        return dto;
    }
}