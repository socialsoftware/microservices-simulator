package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ShippingUpdatedEvent extends Event {
    @Column(name = "shipping_updated_event_address")
    private String address;
    @Column(name = "shipping_updated_event_carrier")
    private String carrier;
    @Column(name = "shipping_updated_event_tracking_number")
    private String trackingNumber;

    public ShippingUpdatedEvent() {
        super();
    }

    public ShippingUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ShippingUpdatedEvent(Integer aggregateId, String address, String carrier, String trackingNumber) {
        super(aggregateId);
        setAddress(address);
        setCarrier(carrier);
        setTrackingNumber(trackingNumber);
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

}