package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;

public class CourseValidationAnnotations {

    public static class NameValidation {
        @NotNull
    @NotBlank
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class TypeValidation {
        @NotNull
        private CourseType type;
        
        public CourseType getType() {
            return type;
        }
        
        public void setType(CourseType type) {
            this.type = type;
        }
    }

    public static class CreationDateValidation {
        @NotNull
        private LocalDateTime creationDate;
        
        public LocalDateTime getCreationDate() {
            return creationDate;
        }
        
        public void setCreationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
        }
    }

}