package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.ShippingStatus;

public class CreateShippingRequestDto {
    @NotNull
    private OrderDto order;
    @NotNull
    private String address;
    @NotNull
    private String carrier;
    @NotNull
    private String trackingNumber;
    @NotNull
    private ShippingStatus status;

    public CreateShippingRequestDto() {}

    public CreateShippingRequestDto(OrderDto order, String address, String carrier, String trackingNumber, ShippingStatus status) {
        this.order = order;
        this.address = address;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.status = status;
    }

    public OrderDto getOrder() {
        return order;
    }

    public void setOrder(OrderDto order) {
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
    public ShippingStatus getStatus() {
        return status;
    }

    public void setStatus(ShippingStatus status) {
        this.status = status;
    }
}
