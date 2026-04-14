package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductCreatedEvent extends Event {
    private String sku;
    private String name;
    private Double priceInCents;

    public ProductCreatedEvent() {
        super();
    }

    public ProductCreatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ProductCreatedEvent(Integer aggregateId, String sku, String name, Double priceInCents) {
        super(aggregateId);
        setSku(sku);
        setName(name);
        setPriceInCents(priceInCents);
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPriceInCents() {
        return priceInCents;
    }

    public void setPriceInCents(Double priceInCents) {
        this.priceInCents = priceInCents;
    }

}