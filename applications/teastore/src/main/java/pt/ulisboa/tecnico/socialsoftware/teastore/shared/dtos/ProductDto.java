package pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;

public class ProductDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Integer categoryAggregateId;
    private String name;
    private String description;
    private Double listPriceInCents;

    public ProductDto() {
    }

    public ProductDto(Product product) {
        this.aggregateId = product.getAggregateId();
        this.version = product.getVersion();
        this.state = product.getState();
        this.categoryAggregateId = product.getProductCategory() != null ? product.getProductCategory().getCategoryAggregateId() : null;
        this.name = product.getName();
        this.description = product.getDescription();
        this.listPriceInCents = product.getListPriceInCents();
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Integer getCategoryAggregateId() {
        return categoryAggregateId;
    }

    public void setCategoryAggregateId(Integer categoryAggregateId) {
        this.categoryAggregateId = categoryAggregateId;
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

    public Double getListPriceInCents() {
        return listPriceInCents;
    }

    public void setListPriceInCents(Double listPriceInCents) {
        this.listPriceInCents = listPriceInCents;
    }
}