package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class RoomUpdatedEvent extends Event {
    @Column(name = "room_updated_event_room_number")
    private String roomNumber;
    @Column(name = "room_updated_event_description")
    private String description;
    @Column(name = "room_updated_event_price_per_night")
    private Double pricePerNight;

    public RoomUpdatedEvent() {
        super();
    }

    public RoomUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public RoomUpdatedEvent(Integer aggregateId, String roomNumber, String description, Double pricePerNight) {
        super(aggregateId);
        setRoomNumber(roomNumber);
        setDescription(description);
        setPricePerNight(pricePerNight);
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

}