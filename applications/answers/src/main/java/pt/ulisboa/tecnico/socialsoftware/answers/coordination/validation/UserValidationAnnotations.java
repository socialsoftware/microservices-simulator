package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.validation.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;


/**
 * Validation annotations for User properties
 */
public class UserValidationAnnotations {

    /**
     * Validation annotations for name
     */
    public static class NameValidation {
        @NotNull
    @NotBlank
        private String name;
        
        // Getter and setter
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Validation annotations for username
     */
    public static class UsernameValidation {
        @NotNull
    @NotBlank
        private String username;
        
        // Getter and setter
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }

    /**
     * Validation annotations for active
     */
    public static class ActiveValidation {
        @NotNull
        private Boolean active;
        
        // Getter and setter
        public Boolean getActive() {
            return active;
        }
        
        public void setActive(Boolean active) {
            this.active = active;
        }
    }

}