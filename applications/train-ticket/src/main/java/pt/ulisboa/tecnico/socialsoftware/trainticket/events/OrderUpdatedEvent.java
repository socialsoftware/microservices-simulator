package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class OrderUpdatedEvent extends Event {
    private String boughtDate;
    private String travelDate;
    private String travelTime;
    private Integer coachNumber;
    private String seatNumber;
    private Double price;

    public OrderUpdatedEvent() {
        super();
    }

    public OrderUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderUpdatedEvent(Integer aggregateId, String boughtDate, String travelDate, String travelTime, Integer coachNumber, String seatNumber, Double price) {
        super(aggregateId);
        setBoughtDate(boughtDate);
        setTravelDate(travelDate);
        setTravelTime(travelTime);
        setCoachNumber(coachNumber);
        setSeatNumber(seatNumber);
        setPrice(price);
    }

    public String getBoughtDate() {
        return boughtDate;
    }

    public void setBoughtDate(String boughtDate) {
        this.boughtDate = boughtDate;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }

    public String getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(String travelTime) {
        this.travelTime = travelTime;
    }

    public Integer getCoachNumber() {
        return coachNumber;
    }

    public void setCoachNumber(Integer coachNumber) {
        this.coachNumber = coachNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

}
