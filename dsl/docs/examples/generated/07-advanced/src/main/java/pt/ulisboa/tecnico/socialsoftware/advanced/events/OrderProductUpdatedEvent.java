package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderProductUpdatedEvent extends Event {
    @Column(name = "order_product_updated_event_product_aggregate_id")
    private Integer productAggregateId;
    @Column(name = "order_product_updated_event_product_version")
    private Integer productVersion;
    @Column(name = "order_product_updated_event_product_name")
    private String productName;
    @Column(name = "order_product_updated_event_product_price")
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