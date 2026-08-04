package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class OrderTripUpdatedEvent extends Event {
    private Integer tripAggregateId;
    private Integer tripVersion;
    private String tripNumber;

    public OrderTripUpdatedEvent() {
        super();
    }

    public OrderTripUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderTripUpdatedEvent(Integer aggregateId, Integer tripAggregateId, Integer tripVersion, String tripNumber) {
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

    public Integer getTripVersion() {
        return tripVersion;
    }

    public void setTripVersion(Integer tripVersion) {
        this.tripVersion = tripVersion;
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

}
