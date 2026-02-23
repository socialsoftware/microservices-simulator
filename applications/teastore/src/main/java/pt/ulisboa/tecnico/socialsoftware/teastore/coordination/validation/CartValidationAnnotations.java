package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;

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