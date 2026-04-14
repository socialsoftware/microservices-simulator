package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class BookingCreatedEvent extends Event {
    private Integer userAggregateId;
    private Integer roomAggregateId;
    private Double totalPrice;

    public BookingCreatedEvent() {
        super();
    }

    public BookingCreatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookingCreatedEvent(Integer aggregateId, Integer userAggregateId, Integer roomAggregateId, Double totalPrice) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setRoomAggregateId(roomAggregateId);
        setTotalPrice(totalPrice);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getRoomAggregateId() {
        return roomAggregateId;
    }

    public void setRoomAggregateId(Integer roomAggregateId) {
        this.roomAggregateId = roomAggregateId;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

}