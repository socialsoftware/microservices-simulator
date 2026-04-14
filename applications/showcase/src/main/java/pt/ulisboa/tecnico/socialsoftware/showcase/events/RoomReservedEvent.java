package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class RoomReservedEvent extends Event {
    private String roomNumber;

    public RoomReservedEvent() {
        super();
    }

    public RoomReservedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public RoomReservedEvent(Integer aggregateId, String roomNumber) {
        super(aggregateId);
        setRoomNumber(roomNumber);
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

}