package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceCustomer;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceOrder;

public class InvoiceValidationAnnotations {

    public static class OrderValidation {
        @NotNull
        private InvoiceOrder order;
        
        public InvoiceOrder getOrder() {
            return order;
        }
        
        public void setOrder(InvoiceOrder order) {
            this.order = order;
        }
    }

    public static class CustomerValidation {
        @NotNull
        private InvoiceCustomer customer;
        
        public InvoiceCustomer getCustomer() {
            return customer;
        }
        
        public void setCustomer(InvoiceCustomer customer) {
            this.customer = customer;
        }
    }

    public static class TotalAmountValidation {
        @NotNull
        private Double totalAmount;
        
        public Double getTotalAmount() {
            return totalAmount;
        }
        
        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    public static class IssuedAtValidation {
        @NotNull
        private LocalDateTime issuedAt;
        
        public LocalDateTime getIssuedAt() {
            return issuedAt;
        }
        
        public void setIssuedAt(LocalDateTime issuedAt) {
            this.issuedAt = issuedAt;
        }
    }

    public static class PaidValidation {
        @NotNull
        private Boolean paid;
        
        public Boolean getPaid() {
            return paid;
        }
        
        public void setPaid(Boolean paid) {
            this.paid = paid;
        }
    }

}