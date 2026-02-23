package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductUpdatedEvent extends Event {
    private String name;
    private Double price;
    private Boolean available;

    public ProductUpdatedEvent() {
        super();
    }

    public ProductUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ProductUpdatedEvent(Integer aggregateId, String name, Double price, Boolean available) {
        super(aggregateId);
        setName(name);
        setPrice(price);
        setAvailable(available);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

}