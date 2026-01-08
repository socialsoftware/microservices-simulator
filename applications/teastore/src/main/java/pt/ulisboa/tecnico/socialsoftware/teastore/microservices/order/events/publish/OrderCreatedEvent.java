package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderCreatedEvent extends Event {
    private Integer userAggregateId;
    private Double totalPriceInCents;
    private String time;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(Integer aggregateId, Integer userAggregateId, Double totalPriceInCents, String time) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setTotalPriceInCents(totalPriceInCents);
        setTime(time);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Double getTotalPriceInCents() {
        return totalPriceInCents;
    }

    public void setTotalPriceInCents(Double totalPriceInCents) {
        this.totalPriceInCents = totalPriceInCents;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}