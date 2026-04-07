package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartUser;

public class CartValidationAnnotations {

    public static class UserValidation {
        @NotNull
        private CartUser user;
        
        public CartUser getUser() {
            return user;
        }
        
        public void setUser(CartUser user) {
            this.user = user;
        }
    }

    public static class TotalInCentsValidation {
        @NotNull
        private Double totalInCents;
        
        public Double getTotalInCents() {
            return totalInCents;
        }
        
        public void setTotalInCents(Double totalInCents) {
            this.totalInCents = totalInCents;
        }
    }

    public static class ItemCountValidation {
        @NotNull
        private Integer itemCount;
        
        public Integer getItemCount() {
            return itemCount;
        }
        
        public void setItemCount(Integer itemCount) {
            this.itemCount = itemCount;
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

}