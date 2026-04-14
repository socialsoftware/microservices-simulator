package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.aggregate.Product;

public class ProductDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String sku;
    private String name;
    private String description;
    private Double priceInCents;
    private Integer stock;

    public ProductDto() {
    }

    public ProductDto(Product product) {
        this.aggregateId = product.getAggregateId();
        this.version = product.getVersion();
        this.state = product.getState();
        this.sku = product.getSku();
        this.name = product.getName();
        this.description = product.getDescription();
        this.priceInCents = product.getPriceInCents();
        this.stock = product.getStock();
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

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public Double getPriceInCents() {
        return priceInCents;
    }

    public void setPriceInCents(Double priceInCents) {
        this.priceInCents = priceInCents;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}