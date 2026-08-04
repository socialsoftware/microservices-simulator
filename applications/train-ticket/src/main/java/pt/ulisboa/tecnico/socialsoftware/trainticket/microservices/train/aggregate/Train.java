package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Train extends Aggregate {
    private String name;
    private Integer economyClass;
    private Integer confortClass;
    private Integer averageSpeed;

    public Train() {

    }

    public Train(Integer aggregateId, TrainDto trainDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(trainDto.getName());
        setEconomyClass(trainDto.getEconomyClass());
        setConfortClass(trainDto.getConfortClass());
        setAverageSpeed(trainDto.getAverageSpeed());
    }


    public Train(Train other) {
        super(other);
        setName(other.getName());
        setEconomyClass(other.getEconomyClass());
        setConfortClass(other.getConfortClass());
        setAverageSpeed(other.getAverageSpeed());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getEconomyClass() {
        return economyClass;
    }

    public void setEconomyClass(Integer economyClass) {
        this.economyClass = economyClass;
    }

    public Integer getConfortClass() {
        return confortClass;
    }

    public void setConfortClass(Integer confortClass) {
        this.confortClass = confortClass;
    }

    public Integer getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Integer averageSpeed) {
        this.averageSpeed = averageSpeed;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantNameNotBlank() {
        return this.name != null && this.name != null && this.name.length() > 0;
    }

    private boolean invariantEconomyClassNonNegative() {
        return economyClass >= 0;
    }

    private boolean invariantConfortClassNonNegative() {
        return confortClass >= 0;
    }

    private boolean invariantAverageSpeedNonNegative() {
        return averageSpeed >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantNameNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Train type name cannot be blank");
        }
        if (!invariantEconomyClassNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Economy-class capacity cannot be negative");
        }
        if (!invariantConfortClassNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "First-class capacity cannot be negative");
        }
        if (!invariantAverageSpeedNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Average speed cannot be negative");
        }
    }

    public TrainDto buildDto() {
        TrainDto dto = new TrainDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setEconomyClass(getEconomyClass());
        dto.setConfortClass(getConfortClass());
        dto.setAverageSpeed(getAverageSpeed());
        return dto;
    }
}