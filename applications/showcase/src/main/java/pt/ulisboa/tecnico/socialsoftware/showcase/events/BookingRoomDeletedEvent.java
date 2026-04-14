package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class BookingRoomDeletedEvent extends Event {
    @Column(name = "booking_room_deleted_event_room_aggregate_id")
    private Integer roomAggregateId;

    public BookingRoomDeletedEvent() {
        super();
    }

    public BookingRoomDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookingRoomDeletedEvent(Integer aggregateId, Integer roomAggregateId) {
        super(aggregateId);
        setRoomAggregateId(roomAggregateId);
    }

    public Integer getRoomAggregateId() {
        return roomAggregateId;
    }

    public void setRoomAggregateId(Integer roomAggregateId) {
        this.roomAggregateId = roomAggregateId;
    }

}