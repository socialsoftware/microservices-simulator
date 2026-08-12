package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class CategoryValidationAnnotations {

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

    public static class DescriptionValidation {
        @NotNull
    @NotBlank
        private String description;
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }

}