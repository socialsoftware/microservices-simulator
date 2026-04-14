package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class RoomAmenityUpdatedEvent extends Event {
    @Column(name = "room_amenity_updated_event_code")
    private Integer code;

    public RoomAmenityUpdatedEvent() {
        super();
    }

    public RoomAmenityUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public RoomAmenityUpdatedEvent(Integer aggregateId, Integer code) {
        super(aggregateId);
        setCode(code);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

}