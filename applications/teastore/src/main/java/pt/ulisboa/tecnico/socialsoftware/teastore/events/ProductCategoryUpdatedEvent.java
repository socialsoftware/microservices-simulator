package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ProductCategoryUpdatedEvent extends Event {
    @Column(name = "product_category_updated_event_category_aggregate_id")
    private Integer categoryAggregateId;
    @Column(name = "product_category_updated_event_category_version")
    private Integer categoryVersion;
    @Column(name = "product_category_updated_event_category_name")
    private String categoryName;
    @Column(name = "product_category_updated_event_category_description")
    private String categoryDescription;

    public ProductCategoryUpdatedEvent() {
        super();
    }

    public ProductCategoryUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ProductCategoryUpdatedEvent(Integer aggregateId, Integer categoryAggregateId, Integer categoryVersion, String categoryName, String categoryDescription) {
        super(aggregateId);
        setCategoryAggregateId(categoryAggregateId);
        setCategoryVersion(categoryVersion);
        setCategoryName(categoryName);
        setCategoryDescription(categoryDescription);
    }

    public Integer getCategoryAggregateId() {
        return categoryAggregateId;
    }

    public void setCategoryAggregateId(Integer categoryAggregateId) {
        this.categoryAggregateId = categoryAggregateId;
    }

    public Integer getCategoryVersion() {
        return categoryVersion;
    }

    public void setCategoryVersion(Integer categoryVersion) {
        this.categoryVersion = categoryVersion;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

}