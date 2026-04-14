package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.subscribe.ShippingSubscribesOrderCancelled;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.subscribe.ShippingSubscribesPaymentAuthorized;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingOrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.ShippingStatus;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Shipping extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "shipping")
    private ShippingOrder order;
    private String address;
    private String carrier;
    private String trackingNumber;
    @Enumerated(EnumType.STRING)
    private ShippingStatus status;

    public Shipping() {

    }

    public Shipping(Integer aggregateId, ShippingDto shippingDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAddress(shippingDto.getAddress());
        setCarrier(shippingDto.getCarrier());
        setTrackingNumber(shippingDto.getTrackingNumber());
        setStatus(ShippingStatus.valueOf(shippingDto.getStatus()));
        setOrder(shippingDto.getOrder() != null ? new ShippingOrder(shippingDto.getOrder()) : null);
    }


    public Shipping(Shipping other) {
        super(other);
        setOrder(other.getOrder() != null ? new ShippingOrder(other.getOrder()) : null);
        setAddress(other.getAddress());
        setCarrier(other.getCarrier());
        setTrackingNumber(other.getTrackingNumber());
        setStatus(other.getStatus());
    }

    public ShippingOrder getOrder() {
        return order;
    }

    public void setOrder(ShippingOrder order) {
        this.order = order;
        if (this.order != null) {
            this.order.setShipping(this);
        }
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            eventSubscriptions.add(new ShippingSubscribesPaymentAuthorized());
            eventSubscriptions.add(new ShippingSubscribesOrderCancelled());
        }
        return eventSubscriptions;
    }



    private boolean invariantRule0() {
        return this.address != null && this.address.length() > 0;
    }

    private boolean invariantRule1() {
        return this.carrier != null && this.carrier.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Shipping address cannot be empty");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Carrier cannot be empty");
        }
    }

    public ShippingDto buildDto() {
        ShippingDto dto = new ShippingDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setOrder(getOrder() != null ? new ShippingOrderDto(getOrder()) : null);
        dto.setAddress(getAddress());
        dto.setCarrier(getCarrier());
        dto.setTrackingNumber(getTrackingNumber());
        dto.setStatus(getStatus() != null ? getStatus().name() : null);
        return dto;
    }
}