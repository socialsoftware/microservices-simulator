package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductCategoryDeletedEvent extends Event {
    private Integer categoryAggregateId;

    public ProductCategoryDeletedEvent() {
        super();
    }

    public ProductCategoryDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ProductCategoryDeletedEvent(Integer aggregateId, Integer categoryAggregateId) {
        super(aggregateId);
        setCategoryAggregateId(categoryAggregateId);
    }

    public Integer getCategoryAggregateId() {
        return categoryAggregateId;
    }

    public void setCategoryAggregateId(Integer categoryAggregateId) {
        this.categoryAggregateId = categoryAggregateId;
    }

}