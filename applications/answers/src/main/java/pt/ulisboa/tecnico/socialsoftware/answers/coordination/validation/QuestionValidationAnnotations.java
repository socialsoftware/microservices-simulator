package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.validation.annotations;

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

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;


/**
 * Validation annotations for Question properties
 */
public class QuestionValidationAnnotations {

    /**
     * Validation annotations for title
     */
    public static class TitleValidation {
        @NotNull
    @NotBlank
        private String title;
        
        // Getter and setter
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * Validation annotations for content
     */
    public static class ContentValidation {
        @NotNull
    @NotBlank
        private String content;
        
        // Getter and setter
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * Validation annotations for numberOfOptions
     */
    public static class NumberOfOptionsValidation {
        @NotNull
        private Integer numberOfOptions;
        
        // Getter and setter
        public Integer getNumberOfOptions() {
            return numberOfOptions;
        }
        
        public void setNumberOfOptions(Integer numberOfOptions) {
            this.numberOfOptions = numberOfOptions;
        }
    }

    /**
     * Validation annotations for correctOption
     */
    public static class CorrectOptionValidation {
        @NotNull
        private Integer correctOption;
        
        // Getter and setter
        public Integer getCorrectOption() {
            return correctOption;
        }
        
        public void setCorrectOption(Integer correctOption) {
            this.correctOption = correctOption;
        }
    }

    /**
     * Validation annotations for order
     */
    public static class OrderValidation {
        @NotNull
        private Integer order;
        
        // Getter and setter
        public Integer getOrder() {
            return order;
        }
        
        public void setOrder(Integer order) {
            this.order = order;
        }
    }

    /**
     * Validation annotations for course
     */
    public static class CourseValidation {
        @NotNull
        private QuestionCourse course;
        
        // Getter and setter
        public QuestionCourse getCourse() {
            return course;
        }
        
        public void setCourse(QuestionCourse course) {
            this.course = course;
        }
    }

    /**
     * Validation annotations for topics
     */
    public static class TopicsValidation {
        @NotNull
    @NotEmpty
        private Set<QuestionTopic> topics;
        
        // Getter and setter
        public Set<QuestionTopic> getTopics() {
            return topics;
        }
        
        public void setTopics(Set<QuestionTopic> topics) {
            this.topics = topics;
        }
    }

    /**
     * Validation annotations for options
     */
    public static class OptionsValidation {
        @NotNull
    @NotEmpty
        private Set<Option> options;
        
        // Getter and setter
        public Set<Option> getOptions() {
            return options;
        }
        
        public void setOptions(Set<Option> options) {
            this.options = options;
        }
    }

}