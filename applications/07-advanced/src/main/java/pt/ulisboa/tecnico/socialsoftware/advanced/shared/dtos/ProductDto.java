package pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.aggregate.Product;

public class ProductDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private Double price;
    private Boolean available;

    public ProductDto() {
    }

    public ProductDto(Product product) {
        this.aggregateId = product.getAggregateId();
        this.version = product.getVersion();
        this.state = product.getState();
        this.name = product.getName();
        this.price = product.getPrice();
        this.available = product.getAvailable();
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}