package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;


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

public class BookValidationAnnotations {

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

    public static class AuthorValidation {
        @NotNull
    @NotBlank
        private String author;
        
        public String getAuthor() {
            return author;
        }
        
        public void setAuthor(String author) {
            this.author = author;
        }
    }

    public static class GenreValidation {
        @NotNull
    @NotBlank
        private String genre;
        
        public String getGenre() {
            return genre;
        }
        
        public void setGenre(String genre) {
            this.genre = genre;
        }
    }

    public static class AvailableValidation {
        @NotNull
        private Boolean available;
        
        public Boolean getAvailable() {
            return available;
        }
        
        public void setAvailable(Boolean available) {
            this.available = available;
        }
    }

}