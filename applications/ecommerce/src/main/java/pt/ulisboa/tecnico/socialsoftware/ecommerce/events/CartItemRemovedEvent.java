package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class CartItemRemovedEvent extends Event {
    @Column(name = "cart_item_removed_event_quantity")
    private Integer quantity;

    public CartItemRemovedEvent() {
        super();
    }

    public CartItemRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CartItemRemovedEvent(Integer aggregateId, Integer quantity) {
        super(aggregateId);
        setQuantity(quantity);
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

}