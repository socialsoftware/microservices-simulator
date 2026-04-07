package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingOrder;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.ShippingStatus;

public class ShippingValidationAnnotations {

    public static class OrderValidation {
        @NotNull
        private ShippingOrder order;
        
        public ShippingOrder getOrder() {
            return order;
        }
        
        public void setOrder(ShippingOrder order) {
            this.order = order;
        }
    }

    public static class AddressValidation {
        @NotNull
    @NotBlank
        private String address;
        
        public String getAddress() {
            return address;
        }
        
        public void setAddress(String address) {
            this.address = address;
        }
    }

    public static class CarrierValidation {
        @NotNull
    @NotBlank
        private String carrier;
        
        public String getCarrier() {
            return carrier;
        }
        
        public void setCarrier(String carrier) {
            this.carrier = carrier;
        }
    }

    public static class TrackingNumberValidation {
        @NotNull
    @NotBlank
        private String trackingNumber;
        
        public String getTrackingNumber() {
            return trackingNumber;
        }
        
        public void setTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
        }
    }

    public static class StatusValidation {
        @NotNull
        private ShippingStatus status;
        
        public ShippingStatus getStatus() {
            return status;
        }
        
        public void setStatus(ShippingStatus status) {
            this.status = status;
        }
    }

}