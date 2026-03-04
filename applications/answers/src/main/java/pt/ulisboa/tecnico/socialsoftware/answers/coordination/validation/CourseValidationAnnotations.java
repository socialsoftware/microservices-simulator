package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
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