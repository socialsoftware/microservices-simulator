package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderUser;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.OrderStatus;

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

    public static class StatusValidation {
        @NotNull
        private OrderStatus status;
        
        public OrderStatus getStatus() {
            return status;
        }
        
        public void setStatus(OrderStatus status) {
            this.status = status;
        }
    }

    public static class PlacedAtValidation {
        @NotNull
    @NotBlank
        private String placedAt;
        
        public String getPlacedAt() {
            return placedAt;
        }
        
        public void setPlacedAt(String placedAt) {
            this.placedAt = placedAt;
        }
    }

}