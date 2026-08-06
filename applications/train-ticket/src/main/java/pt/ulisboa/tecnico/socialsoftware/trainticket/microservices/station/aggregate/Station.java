package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Station extends Aggregate {
    private String name;
    private Integer stayTime;

    public Station() {

    }

    public Station(Integer aggregateId, StationDto stationDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(stationDto.getName());
        setStayTime(stationDto.getStayTime());
    }


    public Station(Station other) {
        super(other);
        setName(other.getName());
        setStayTime(other.getStayTime());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStayTime() {
        return stayTime;
    }

    public void setStayTime(Integer stayTime) {
        this.stayTime = stayTime;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantNameNotBlank() {
        return this.name != null && this.name != null && this.name.length() > 0;
    }

    private boolean invariantStayTimeNonNegative() {
        return stayTime >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantNameNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Station name cannot be blank");
        }
        if (!invariantStayTimeNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Station stay time cannot be negative");
        }
    }

    public StationDto buildDto() {
        StationDto dto = new StationDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setStayTime(getStayTime());
        return dto;
    }
}