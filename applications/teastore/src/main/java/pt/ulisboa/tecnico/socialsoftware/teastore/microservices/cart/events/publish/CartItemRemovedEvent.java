package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CartItemRemovedEvent extends Event {
    private Long productId;

    public CartItemRemovedEvent() {
    }

    public CartItemRemovedEvent(Integer aggregateId, Long productId) {
        super(aggregateId);
        setProductId(productId);
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

}