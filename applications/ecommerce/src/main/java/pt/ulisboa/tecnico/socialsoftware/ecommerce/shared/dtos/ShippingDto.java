package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;

public class ShippingDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private ShippingOrderDto order;
    private String address;
    private String carrier;
    private String trackingNumber;
    private String status;

    public ShippingDto() {
    }

    public ShippingDto(Shipping shipping) {
        this.aggregateId = shipping.getAggregateId();
        this.version = shipping.getVersion();
        this.state = shipping.getState();
        this.order = shipping.getOrder() != null ? new ShippingOrderDto(shipping.getOrder()) : null;
        this.address = shipping.getAddress();
        this.carrier = shipping.getCarrier();
        this.trackingNumber = shipping.getTrackingNumber();
        this.status = shipping.getStatus() != null ? shipping.getStatus().name() : null;
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

    public ShippingOrderDto getOrder() {
        return order;
    }

    public void setOrder(ShippingOrderDto order) {
        this.order = order;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}