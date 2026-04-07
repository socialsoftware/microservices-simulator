package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentOrder;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.PaymentStatus;

public class PaymentValidationAnnotations {

    public static class OrderValidation {
        @NotNull
        private PaymentOrder order;
        
        public PaymentOrder getOrder() {
            return order;
        }
        
        public void setOrder(PaymentOrder order) {
            this.order = order;
        }
    }

    public static class AmountInCentsValidation {
        @NotNull
        private Double amountInCents;
        
        public Double getAmountInCents() {
            return amountInCents;
        }
        
        public void setAmountInCents(Double amountInCents) {
            this.amountInCents = amountInCents;
        }
    }

    public static class StatusValidation {
        @NotNull
        private PaymentStatus status;
        
        public PaymentStatus getStatus() {
            return status;
        }
        
        public void setStatus(PaymentStatus status) {
            this.status = status;
        }
    }

    public static class AuthorizationCodeValidation {
        @NotNull
    @NotBlank
        private String authorizationCode;
        
        public String getAuthorizationCode() {
            return authorizationCode;
        }
        
        public void setAuthorizationCode(String authorizationCode) {
            this.authorizationCode = authorizationCode;
        }
    }

    public static class PaymentMethodValidation {
        @NotNull
    @NotBlank
        private String paymentMethod;
        
        public String getPaymentMethod() {
            return paymentMethod;
        }
        
        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }

}