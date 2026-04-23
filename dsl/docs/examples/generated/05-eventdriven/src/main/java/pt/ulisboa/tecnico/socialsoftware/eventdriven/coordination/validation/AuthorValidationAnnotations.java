package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class AuthorValidationAnnotations {

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

    public static class BioValidation {
        @NotNull
    @NotBlank
        private String bio;
        
        public String getBio() {
            return bio;
        }
        
        public void setBio(String bio) {
            this.bio = bio;
        }
    }

}