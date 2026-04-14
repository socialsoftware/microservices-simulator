package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class BookingRoomUpdatedEvent extends Event {
    @Column(name = "booking_room_updated_event_room_aggregate_id")
    private Integer roomAggregateId;
    @Column(name = "booking_room_updated_event_room_version")
    private Integer roomVersion;
    @Column(name = "booking_room_updated_event_room_number")
    private String roomNumber;
    @Column(name = "booking_room_updated_event_price_per_night")
    private Double pricePerNight;

    public BookingRoomUpdatedEvent() {
        super();
    }

    public BookingRoomUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookingRoomUpdatedEvent(Integer aggregateId, Integer roomAggregateId, Integer roomVersion, String roomNumber, Double pricePerNight) {
        super(aggregateId);
        setRoomAggregateId(roomAggregateId);
        setRoomVersion(roomVersion);
        setRoomNumber(roomNumber);
        setPricePerNight(pricePerNight);
    }

    public Integer getRoomAggregateId() {
        return roomAggregateId;
    }

    public void setRoomAggregateId(Integer roomAggregateId) {
        this.roomAggregateId = roomAggregateId;
    }

    public Integer getRoomVersion() {
        return roomVersion;
    }

    public void setRoomVersion(Integer roomVersion) {
        this.roomVersion = roomVersion;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

}