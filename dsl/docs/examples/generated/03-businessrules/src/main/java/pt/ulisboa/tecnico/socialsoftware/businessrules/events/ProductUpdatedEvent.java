package pt.ulisboa.tecnico.socialsoftware.businessrules.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ProductUpdatedEvent extends Event {
    @Column(name = "product_updated_event_name")
    private String name;
    @Column(name = "product_updated_event_sku")
    private String sku;
    @Column(name = "product_updated_event_price")
    private Double price;
    @Column(name = "product_updated_event_stock_quantity")
    private Integer stockQuantity;
    @Column(name = "product_updated_event_active")
    private Boolean active;

    public ProductUpdatedEvent() {
        super();
    }

    public ProductUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ProductUpdatedEvent(Integer aggregateId, String name, String sku, Double price, Integer stockQuantity, Boolean active) {
        super(aggregateId);
        setName(name);
        setSku(sku);
        setPrice(price);
        setStockQuantity(stockQuantity);
        setActive(active);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}