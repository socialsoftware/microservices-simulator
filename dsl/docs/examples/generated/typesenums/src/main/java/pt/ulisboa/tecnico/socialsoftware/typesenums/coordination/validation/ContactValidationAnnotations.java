package pt.ulisboa.tecnico.socialsoftware.typesenums.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.enums.ContactCategory;

public class ContactValidationAnnotations {

    public static class FirstNameValidation {
        @NotNull
    @NotBlank
        private String firstName;
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    public static class LastNameValidation {
        @NotNull
    @NotBlank
        private String lastName;
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static class EmailValidation {
        @NotNull
    @NotBlank
    @Email
        private String email;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class CategoryValidation {
        @NotNull
        private ContactCategory category;
        
        public ContactCategory getCategory() {
            return category;
        }
        
        public void setCategory(ContactCategory category) {
            this.category = category;
        }
    }

    public static class CreatedAtValidation {
        @NotNull
        private LocalDateTime createdAt;
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class FavoriteValidation {
        @NotNull
        private Boolean favorite;
        
        public Boolean getFavorite() {
            return favorite;
        }
        
        public void setFavorite(Boolean favorite) {
            this.favorite = favorite;
        }
    }

    public static class CallCountValidation {
        @NotNull
        private Integer callCount;
        
        public Integer getCallCount() {
            return callCount;
        }
        
        public void setCallCount(Integer callCount) {
            this.callCount = callCount;
        }
    }

}