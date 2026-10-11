package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "train_types")
public abstract class TrainType extends Aggregate {
    private final String name;
    private Integer economyClassSeats;
    private Integer firstClassSeats;
    private Integer averageSpeed;

    public TrainType() {
        this.name = null;
    }

    public TrainType(Integer aggregateId, String name, Integer economyClassSeats, Integer firstClassSeats,
                     Integer averageSpeed) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = name;
        this.economyClassSeats = economyClassSeats;
        this.firstClassSeats = firstClassSeats;
        this.averageSpeed = averageSpeed;
    }

    public TrainType(TrainType other) {
        super(other);
        this.name = other.getName();
        this.economyClassSeats = other.getEconomyClassSeats();
        this.firstClassSeats = other.getFirstClassSeats();
        this.averageSpeed = other.getAverageSpeed();
    }

    @Override
    public void verifyInvariants() {
        // TRAIN_TYPE_NAME_FINAL is enforced by the `final` field above;
        seatsNonNegative();
        speedPositive();
    }

    private void seatsNonNegative() {
        if (this.economyClassSeats == null || this.economyClassSeats < 0
                || this.firstClassSeats == null || this.firstClassSeats < 0) {
            throw new TrainTicketException(TrainTicketErrorMessage.TRAIN_TYPE_SEATS_NON_NEGATIVE);
        }
    }

    private void speedPositive() {
        if (this.averageSpeed == null || this.averageSpeed <= 0) {
            throw new TrainTicketException(TrainTicketErrorMessage.TRAIN_TYPE_SPEED_POSITIVE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    public String getName() { return name; }
    public Integer getEconomyClassSeats() { return economyClassSeats; }
    public void setEconomyClassSeats(Integer economyClassSeats) { this.economyClassSeats = economyClassSeats; }
    public Integer getFirstClassSeats() { return firstClassSeats; }
    public void setFirstClassSeats(Integer firstClassSeats) { this.firstClassSeats = firstClassSeats; }
    public Integer getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(Integer averageSpeed) { this.averageSpeed = averageSpeed; }
}
