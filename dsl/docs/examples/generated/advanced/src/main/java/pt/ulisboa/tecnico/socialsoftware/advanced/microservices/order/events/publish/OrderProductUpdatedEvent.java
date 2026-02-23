package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderProductUpdatedEvent extends Event {
    private Integer productAggregateId;
    private Integer productVersion;
    private String productName;
    private Double productPrice;

    public OrderProductUpdatedEvent() {
        super();
    }

    public OrderProductUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderProductUpdatedEvent(Integer aggregateId, Integer productAggregateId, Integer productVersion, String productName, Double productPrice) {
        super(aggregateId);
        setProductAggregateId(productAggregateId);
        setProductVersion(productVersion);
        setProductName(productName);
        setProductPrice(productPrice);
    }

    public Integer getProductAggregateId() {
        return productAggregateId;
    }

    public void setProductAggregateId(Integer productAggregateId) {
        this.productAggregateId = productAggregateId;
    }

    public Integer getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(Integer productVersion) {
        this.productVersion = productVersion;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(Double productPrice) {
        this.productPrice = productPrice;
    }

}