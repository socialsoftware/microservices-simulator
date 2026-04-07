package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductUpdatedEvent extends Event {
    private String sku;
    private String name;
    private Double priceInCents;
    private Integer stock;

    public ProductUpdatedEvent() {
        super();
    }

    public ProductUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ProductUpdatedEvent(Integer aggregateId, String sku, String name, Double priceInCents, Integer stock) {
        super(aggregateId);
        setSku(sku);
        setName(name);
        setPriceInCents(priceInCents);
        setStock(stock);
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

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

}