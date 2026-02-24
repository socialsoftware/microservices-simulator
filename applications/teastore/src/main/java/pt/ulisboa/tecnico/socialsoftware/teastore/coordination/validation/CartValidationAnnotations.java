package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class CartValidationAnnotations {

    public static class UserIdValidation {
        @NotNull
        private Long userId;
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    public static class CheckedOutValidation {
        @NotNull
        private Boolean checkedOut;
        
        public Boolean getCheckedOut() {
            return checkedOut;
        }
        
        public void setCheckedOut(Boolean checkedOut) {
            this.checkedOut = checkedOut;
        }
    }

    public static class TotalPriceValidation {
        @NotNull
        private Double totalPrice;
        
        public Double getTotalPrice() {
            return totalPrice;
        }
        
        public void setTotalPrice(Double totalPrice) {
            this.totalPrice = totalPrice;
        }
    }

}