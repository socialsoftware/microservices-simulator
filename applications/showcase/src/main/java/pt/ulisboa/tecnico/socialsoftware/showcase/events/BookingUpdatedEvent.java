package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class BookingUpdatedEvent extends Event {
    @Column(name = "booking_updated_event_check_in_date")
    private String checkInDate;
    @Column(name = "booking_updated_event_check_out_date")
    private String checkOutDate;
    @Column(name = "booking_updated_event_number_of_nights")
    private Integer numberOfNights;
    @Column(name = "booking_updated_event_total_price")
    private Double totalPrice;
    @Column(name = "booking_updated_event_confirmed")
    private Boolean confirmed;

    public BookingUpdatedEvent() {
        super();
    }

    public BookingUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookingUpdatedEvent(Integer aggregateId, String checkInDate, String checkOutDate, Integer numberOfNights, Double totalPrice, Boolean confirmed) {
        super(aggregateId);
        setCheckInDate(checkInDate);
        setCheckOutDate(checkOutDate);
        setNumberOfNights(numberOfNights);
        setTotalPrice(totalPrice);
        setConfirmed(confirmed);
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(Integer numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

}