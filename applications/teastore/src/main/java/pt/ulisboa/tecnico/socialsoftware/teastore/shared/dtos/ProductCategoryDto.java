package pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductCategory;

public class ProductCategoryDto implements Serializable {
    private String name;
    private String description;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public ProductCategoryDto() {
    }

    public ProductCategoryDto(ProductCategory productCategory) {
        this.name = productCategory.getCategoryName();
        this.description = productCategory.getCategoryDescription();
        this.aggregateId = productCategory.getCategoryAggregateId();
        this.version = productCategory.getCategoryVersion();
        this.state = productCategory.getCategoryState() != null ? productCategory.getCategoryState().name() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}