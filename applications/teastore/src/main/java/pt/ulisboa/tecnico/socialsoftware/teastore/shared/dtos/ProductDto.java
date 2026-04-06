package pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;

public class ProductDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private ProductCategoryDto productCategory;
    private String name;
    private String description;
    private Double listPriceInCents;

    public ProductDto() {
    }

    public ProductDto(Product product) {
        this.aggregateId = product.getAggregateId();
        this.version = product.getVersion();
        this.state = product.getState();
        this.productCategory = product.getProductCategory() != null ? new ProductCategoryDto(product.getProductCategory()) : null;
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

    public ProductCategoryDto getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategoryDto productCategory) {
        this.productCategory = productCategory;
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