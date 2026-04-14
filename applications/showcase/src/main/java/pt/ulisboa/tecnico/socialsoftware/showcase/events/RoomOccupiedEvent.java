package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class RoomOccupiedEvent extends Event {
    private String roomNumber;

    public RoomOccupiedEvent() {
        super();
    }

    public RoomOccupiedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public RoomOccupiedEvent(Integer aggregateId, String roomNumber) {
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