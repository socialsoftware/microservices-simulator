package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.STATION_STAY_TIME_NON_NEGATIVE;

@Entity
@Table(name = "stations")
public abstract class Station extends Aggregate {
    private String name;
    @Column(name = "stay_time")
    private Integer stayTime;

    public Station() {
    }

    public Station(Integer aggregateId, StationDto stationDto) {
        super(aggregateId);
        setName(stationDto.getName());
        setStayTime(stationDto.getStayTime());
        setAggregateType(getClass().getSimpleName());
    }

    public Station(Station other) {
        super(other);
        setName(other.getName());
        setStayTime(other.getStayTime());
    }

    @Override
    public void verifyInvariants() {
        if (this.stayTime == null || this.stayTime < 0) {
            throw new TrainticketException(STATION_STAY_TIME_NON_NEGATIVE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStayTime() {
        return this.stayTime;
    }

    public void setStayTime(Integer stayTime) {
        this.stayTime = stayTime;
    }
}
