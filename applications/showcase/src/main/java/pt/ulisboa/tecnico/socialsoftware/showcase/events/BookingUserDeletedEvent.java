package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class BookingUserDeletedEvent extends Event {
    @Column(name = "booking_user_deleted_event_user_aggregate_id")
    private Integer userAggregateId;

    public BookingUserDeletedEvent() {
        super();
    }

    public BookingUserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookingUserDeletedEvent(Integer aggregateId, Integer userAggregateId) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

}