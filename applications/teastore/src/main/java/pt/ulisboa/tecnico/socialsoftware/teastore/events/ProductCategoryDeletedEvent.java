package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ProductCategoryDeletedEvent extends Event {
    @Column(name = "product_category_deleted_event_category_aggregate_id")
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