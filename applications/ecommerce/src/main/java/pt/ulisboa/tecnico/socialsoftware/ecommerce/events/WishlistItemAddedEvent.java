package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class WishlistItemAddedEvent extends Event {
    private Integer userAggregateId;
    private Integer productAggregateId;

    public WishlistItemAddedEvent() {
        super();
    }

    public WishlistItemAddedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public WishlistItemAddedEvent(Integer aggregateId, Integer userAggregateId, Integer productAggregateId) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setProductAggregateId(productAggregateId);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getProductAggregateId() {
        return productAggregateId;
    }

    public void setProductAggregateId(Integer productAggregateId) {
        this.productAggregateId = productAggregateId;
    }

}