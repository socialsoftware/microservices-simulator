package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CartItemUpdatedEvent extends Event {
    private Long productId;
    private Integer quantity;

    public CartItemUpdatedEvent() {
    }

    public CartItemUpdatedEvent(Integer aggregateId, Long productId, Integer quantity) {
        super(aggregateId);
        setProductId(productId);
        setQuantity(quantity);
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

}