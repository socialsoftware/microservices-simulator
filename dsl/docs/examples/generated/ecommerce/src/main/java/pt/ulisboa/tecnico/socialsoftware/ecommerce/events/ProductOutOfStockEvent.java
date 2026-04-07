package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductOutOfStockEvent extends Event {
    private String sku;

    public ProductOutOfStockEvent() {
        super();
    }

    public ProductOutOfStockEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ProductOutOfStockEvent(Integer aggregateId, String sku) {
        super(aggregateId);
        setSku(sku);
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

}