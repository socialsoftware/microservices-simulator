package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRIP_START_BEFORE_END;

@Entity
@Table(name = "trips")
public abstract class Trip extends Aggregate {
    // TRIP_NUMBER_FINAL: enforced by the compiler, so there is no setter and no runtime check.
    @Column(name = "trip_number")
    private final String tripNumber;
    // The route and train type references are immutable (domain model §2), so they are final and have no setters.
    @Column(name = "route_aggregate_id")
    private final Integer routeAggregateId;
    @Column(name = "train_type_aggregate_id")
    private final Integer trainTypeAggregateId;
    @Column(name = "start_time")
    private LocalTime startTime;
    @Column(name = "end_time")
    private LocalTime endTime;

    public Trip() {
        this.tripNumber = null;
        this.routeAggregateId = null;
        this.trainTypeAggregateId = null;
    }

    public Trip(Integer aggregateId, TripDto tripDto) {
        super(aggregateId);
        this.tripNumber = tripDto.getTripNumber();
        this.routeAggregateId = tripDto.getRouteAggregateId();
        this.trainTypeAggregateId = tripDto.getTrainTypeAggregateId();
        setStartTime(tripDto.getStartTime());
        setEndTime(tripDto.getEndTime());
        setAggregateType(getClass().getSimpleName());
    }

    public Trip(Trip other) {
        super(other);
        this.tripNumber = other.getTripNumber();
        this.routeAggregateId = other.getRouteAggregateId();
        this.trainTypeAggregateId = other.getTrainTypeAggregateId();
        setStartTime(other.getStartTime());
        setEndTime(other.getEndTime());
    }

    @Override
    public void verifyInvariants() {
        if (this.startTime == null || this.endTime == null || !this.startTime.isBefore(this.endTime)) {
            throw new TrainticketException(TRIP_START_BEFORE_END);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getTripNumber() {
        return this.tripNumber;
    }

    public Integer getRouteAggregateId() {
        return this.routeAggregateId;
    }

    public Integer getTrainTypeAggregateId() {
        return this.trainTypeAggregateId;
    }

    public LocalTime getStartTime() {
        return this.startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return this.endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
