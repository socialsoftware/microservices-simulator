package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceOrder;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceUser;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.InvoiceStatus;

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

    public static class UserValidation {
        @NotNull
        private InvoiceUser user;
        
        public InvoiceUser getUser() {
            return user;
        }
        
        public void setUser(InvoiceUser user) {
            this.user = user;
        }
    }

    public static class InvoiceNumberValidation {
        @NotNull
    @NotBlank
        private String invoiceNumber;
        
        public String getInvoiceNumber() {
            return invoiceNumber;
        }
        
        public void setInvoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
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

    public static class IssuedAtValidation {
        @NotNull
    @NotBlank
        private String issuedAt;
        
        public String getIssuedAt() {
            return issuedAt;
        }
        
        public void setIssuedAt(String issuedAt) {
            this.issuedAt = issuedAt;
        }
    }

    public static class StatusValidation {
        @NotNull
        private InvoiceStatus status;
        
        public InvoiceStatus getStatus() {
            return status;
        }
        
        public void setStatus(InvoiceStatus status) {
            this.status = status;
        }
    }

}