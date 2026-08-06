package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class OrderTripUpdatedEvent extends Event {
    private Integer tripAggregateId;
    private Long tripVersion;
    private String tripNumber;

    public OrderTripUpdatedEvent() {
        super();
    }

    public OrderTripUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderTripUpdatedEvent(Integer aggregateId, Integer tripAggregateId, Long tripVersion, String tripNumber) {
        super(aggregateId);
        setTripAggregateId(tripAggregateId);
        setTripVersion(tripVersion);
        setTripNumber(tripNumber);
    }

    public Integer getTripAggregateId() {
        return tripAggregateId;
    }

    public void setTripAggregateId(Integer tripAggregateId) {
        this.tripAggregateId = tripAggregateId;
    }

    public Long getTripVersion() {
        return tripVersion;
    }

    public void setTripVersion(Long tripVersion) {
        this.tripVersion = tripVersion;
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

}
