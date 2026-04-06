package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ProductUpdatedEvent extends Event {
    @Column(name = "product_updated_event_name")
    private String name;
    @Column(name = "product_updated_event_price")
    private Double price;
    @Column(name = "product_updated_event_available")
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