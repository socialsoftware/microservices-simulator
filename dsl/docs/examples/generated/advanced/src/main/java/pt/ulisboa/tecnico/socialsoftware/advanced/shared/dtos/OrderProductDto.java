package pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderProduct;

public class OrderProductDto implements Serializable {
    private String name;
    private Double price;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public OrderProductDto() {
    }

    public OrderProductDto(OrderProduct orderProduct) {
        this.name = orderProduct.getProductName();
        this.price = orderProduct.getProductPrice();
        this.aggregateId = orderProduct.getProductAggregateId();
        this.version = orderProduct.getProductVersion();
        this.state = orderProduct.getProductState();
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
}