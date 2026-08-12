package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;

public class QuestionValidationAnnotations {

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

    public static class CourseValidation {
        @NotNull
        private QuestionCourse course;
        
        public QuestionCourse getCourse() {
            return course;
        }
        
        public void setCourse(QuestionCourse course) {
            this.course = course;
        }
    }

    public static class TopicsValidation {
        @NotNull
    @NotEmpty
        private Set<QuestionTopic> topics;
        
        public Set<QuestionTopic> getTopics() {
            return topics;
        }
        
        public void setTopics(Set<QuestionTopic> topics) {
            this.topics = topics;
        }
    }

    public static class OptionsValidation {
        @NotNull
    @NotEmpty
        private List<Option> options;
        
        public List<Option> getOptions() {
            return options;
        }
        
        public void setOptions(List<Option> options) {
            this.options = options;
        }
    }

}