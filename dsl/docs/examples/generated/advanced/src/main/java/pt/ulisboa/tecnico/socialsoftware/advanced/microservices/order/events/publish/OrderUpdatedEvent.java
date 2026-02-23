package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class OrderUpdatedEvent extends Event {
    private Double totalAmount;
    private LocalDateTime orderDate;

    public OrderUpdatedEvent() {
        super();
    }

    public OrderUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderUpdatedEvent(Integer aggregateId, Double totalAmount, LocalDateTime orderDate) {
        super(aggregateId);
        setTotalAmount(totalAmount);
        setOrderDate(orderDate);
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

}