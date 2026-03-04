package pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate.Product;

public class ProductDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String sku;
    private Double price;
    private Integer stockQuantity;
    private Boolean active;

    public ProductDto() {
    }

    public ProductDto(Product product) {
        this.aggregateId = product.getAggregateId();
        this.version = product.getVersion();
        this.state = product.getState();
        this.name = product.getName();
        this.sku = product.getSku();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.active = product.getActive();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}