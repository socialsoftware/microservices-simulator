package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;

public class TopicValidationAnnotations {

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

    public static class CourseValidation {
        @NotNull
        private TopicCourse course;
        
        public TopicCourse getCourse() {
            return course;
        }
        
        public void setCourse(TopicCourse course) {
            this.course = course;
        }
    }

}