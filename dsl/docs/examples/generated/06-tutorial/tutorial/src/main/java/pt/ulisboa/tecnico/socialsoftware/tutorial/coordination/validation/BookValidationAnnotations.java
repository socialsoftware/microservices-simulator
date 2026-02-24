package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

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