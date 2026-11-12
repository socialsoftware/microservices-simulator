package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRAIN_TYPE_HAS_SEATS;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRAIN_TYPE_SEATS_NON_NEGATIVE;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRAIN_TYPE_SPEED_POSITIVE;

@Entity
@Table(name = "train_types")
public abstract class TrainType extends Aggregate {
    // TRAIN_TYPE_NAME_FINAL: enforced by the compiler, so there is no setter and no runtime check.
    private final String name;
    @Column(name = "economy_class_seats")
    private Integer economyClassSeats;
    @Column(name = "first_class_seats")
    private Integer firstClassSeats;
    @Column(name = "average_speed")
    private Integer averageSpeed;

    public TrainType() {
        this.name = null;
    }

    public TrainType(Integer aggregateId, TrainTypeDto trainTypeDto) {
        super(aggregateId);
        this.name = trainTypeDto.getName();
        setEconomyClassSeats(trainTypeDto.getEconomyClassSeats());
        setFirstClassSeats(trainTypeDto.getFirstClassSeats());
        setAverageSpeed(trainTypeDto.getAverageSpeed());
        setAggregateType(getClass().getSimpleName());
    }

    public TrainType(TrainType other) {
        super(other);
        this.name = other.getName();
        setEconomyClassSeats(other.getEconomyClassSeats());
        setFirstClassSeats(other.getFirstClassSeats());
        setAverageSpeed(other.getAverageSpeed());
    }

    @Override
    public void verifyInvariants() {
        if (this.economyClassSeats == null || this.economyClassSeats < 0
                || this.firstClassSeats == null || this.firstClassSeats < 0) {
            throw new TrainticketException(TRAIN_TYPE_SEATS_NON_NEGATIVE);
        }
        if (this.economyClassSeats + this.firstClassSeats <= 0) {
            throw new TrainticketException(TRAIN_TYPE_HAS_SEATS);
        }
        if (this.averageSpeed == null || this.averageSpeed <= 0) {
            throw new TrainticketException(TRAIN_TYPE_SPEED_POSITIVE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getName() {
        return this.name;
    }

    public Integer getEconomyClassSeats() {
        return this.economyClassSeats;
    }

    public void setEconomyClassSeats(Integer economyClassSeats) {
        this.economyClassSeats = economyClassSeats;
    }

    public Integer getFirstClassSeats() {
        return this.firstClassSeats;
    }

    public void setFirstClassSeats(Integer firstClassSeats) {
        this.firstClassSeats = firstClassSeats;
    }

    public Integer getAverageSpeed() {
        return this.averageSpeed;
    }

    public void setAverageSpeed(Integer averageSpeed) {
        this.averageSpeed = averageSpeed;
    }
}
