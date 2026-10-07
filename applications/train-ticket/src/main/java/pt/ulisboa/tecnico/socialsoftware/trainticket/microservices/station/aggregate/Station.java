package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "stations")
public abstract class Station extends Aggregate {
    private final String name;
    private Integer stayTime;

    public Station() {
        this.name = null;
    }

    public Station(Integer aggregateId, String name, Integer stayTime) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = name;
        this.stayTime = stayTime;
    }

    public Station(Station other) {
        super(other);
        this.name = other.getName();
        this.stayTime = other.getStayTime();
    }

    @Override
    public void verifyInvariants() {
        // STATION_NAME_FINAL is enforced by the `final` field above;
        stayTimeNonNegative();
    }

    private void stayTimeNonNegative() {
        if (this.stayTime == null || this.stayTime < 0) {
            throw new TrainTicketException(TrainTicketErrorMessage.STATION_STAY_TIME_NON_NEGATIVE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    public String getName() { return name; }
    public Integer getStayTime() { return stayTime; }
    public void setStayTime(Integer stayTime) { this.stayTime = stayTime; }
}
