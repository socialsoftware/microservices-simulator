package pt.ulisboa.tecnico.socialsoftware.helloworld.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class TaskValidationAnnotations {

    public static class TitleValidation {
        @NotNull
    @NotBlank
        private String title;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
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

    public static class DoneValidation {
        @NotNull
        private Boolean done;
        
        public Boolean getDone() {
            return done;
        }
        
        public void setDone(Boolean done) {
            this.done = done;
        }
    }

}