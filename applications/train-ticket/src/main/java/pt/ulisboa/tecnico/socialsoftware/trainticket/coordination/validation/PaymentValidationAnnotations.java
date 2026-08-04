package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentOrder;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentUser;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.PaymentType;

public class PaymentValidationAnnotations {

    public static class AmountValidation {
        @NotNull
        private Double amount;
        
        public Double getAmount() {
            return amount;
        }
        
        public void setAmount(Double amount) {
            this.amount = amount;
        }
    }

    public static class TypeValidation {
        @NotNull
        private PaymentType type;
        
        public PaymentType getType() {
            return type;
        }
        
        public void setType(PaymentType type) {
            this.type = type;
        }
    }

    public static class PaymentDateValidation {
        @NotNull
    @NotBlank
        private String paymentDate;
        
        public String getPaymentDate() {
            return paymentDate;
        }
        
        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }
    }

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

    public static class UserValidation {
        @NotNull
        private PaymentUser user;
        
        public PaymentUser getUser() {
            return user;
        }
        
        public void setUser(PaymentUser user) {
            this.user = user;
        }
    }

}