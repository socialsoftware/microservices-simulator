package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderUser;

public class OrderValidationAnnotations {

    public static class UserValidation {
        @NotNull
        private OrderUser user;
        
        public OrderUser getUser() {
            return user;
        }
        
        public void setUser(OrderUser user) {
            this.user = user;
        }
    }

    public static class TimeValidation {
        @NotNull
    @NotBlank
        private String time;
        
        public String getTime() {
            return time;
        }
        
        public void setTime(String time) {
            this.time = time;
        }
    }

    public static class TotalPriceInCentsValidation {
        @NotNull
        private Double totalPriceInCents;
        
        public Double getTotalPriceInCents() {
            return totalPriceInCents;
        }
        
        public void setTotalPriceInCents(Double totalPriceInCents) {
            this.totalPriceInCents = totalPriceInCents;
        }
    }

    public static class AddressNameValidation {
        @NotNull
    @NotBlank
        private String addressName;
        
        public String getAddressName() {
            return addressName;
        }
        
        public void setAddressName(String addressName) {
            this.addressName = addressName;
        }
    }

    public static class Address1Validation {
        @NotNull
    @NotBlank
        private String address1;
        
        public String getAddress1() {
            return address1;
        }
        
        public void setAddress1(String address1) {
            this.address1 = address1;
        }
    }

    public static class Address2Validation {
        @NotNull
    @NotBlank
        private String address2;
        
        public String getAddress2() {
            return address2;
        }
        
        public void setAddress2(String address2) {
            this.address2 = address2;
        }
    }

    public static class CreditCardCompanyValidation {
        @NotNull
    @NotBlank
        private String creditCardCompany;
        
        public String getCreditCardCompany() {
            return creditCardCompany;
        }
        
        public void setCreditCardCompany(String creditCardCompany) {
            this.creditCardCompany = creditCardCompany;
        }
    }

    public static class CreditCardNumberValidation {
        @NotNull
    @NotBlank
        private String creditCardNumber;
        
        public String getCreditCardNumber() {
            return creditCardNumber;
        }
        
        public void setCreditCardNumber(String creditCardNumber) {
            this.creditCardNumber = creditCardNumber;
        }
    }

    public static class CreditCardExpiryDateValidation {
        @NotNull
    @NotBlank
        private String creditCardExpiryDate;
        
        public String getCreditCardExpiryDate() {
            return creditCardExpiryDate;
        }
        
        public void setCreditCardExpiryDate(String creditCardExpiryDate) {
            this.creditCardExpiryDate = creditCardExpiryDate;
        }
    }

}