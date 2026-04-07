package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderPlacedEvent extends Event {
    private Integer userAggregateId;
    private Double totalInCents;

    public OrderPlacedEvent() {
        super();
    }

    public OrderPlacedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderPlacedEvent(Integer aggregateId, Integer userAggregateId, Double totalInCents) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setTotalInCents(totalInCents);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
    }

}