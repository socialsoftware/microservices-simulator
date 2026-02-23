package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostAuthor;

public class PostValidationAnnotations {

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

    public static class ContentValidation {
        @NotNull
    @NotBlank
        private String content;
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class AuthorValidation {
        @NotNull
        private PostAuthor author;
        
        public PostAuthor getAuthor() {
            return author;
        }
        
        public void setAuthor(PostAuthor author) {
            this.author = author;
        }
    }

    public static class PublishedAtValidation {
        @NotNull
        private LocalDateTime publishedAt;
        
        public LocalDateTime getPublishedAt() {
            return publishedAt;
        }
        
        public void setPublishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
        }
    }

}