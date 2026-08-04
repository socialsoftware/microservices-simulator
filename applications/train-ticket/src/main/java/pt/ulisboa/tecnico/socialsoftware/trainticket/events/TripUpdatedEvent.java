package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class TripUpdatedEvent extends Event {
    private String tripNumber;
    private String startTime;
    private String endTime;

    public TripUpdatedEvent() {
        super();
    }

    public TripUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripUpdatedEvent(Integer aggregateId, String tripNumber, String startTime, String endTime) {
        super(aggregateId);
        setTripNumber(tripNumber);
        setStartTime(startTime);
        setEndTime(endTime);
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

}
