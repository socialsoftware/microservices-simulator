package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.CourseTeacher;

public class CourseValidationAnnotations {

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

    public static class MaxStudentsValidation {
        @NotNull
        private Integer maxStudents;
        
        public Integer getMaxStudents() {
            return maxStudents;
        }
        
        public void setMaxStudents(Integer maxStudents) {
            this.maxStudents = maxStudents;
        }
    }

    public static class TeacherValidation {
        @NotNull
        private CourseTeacher teacher;
        
        public CourseTeacher getTeacher() {
            return teacher;
        }
        
        public void setTeacher(CourseTeacher teacher) {
            this.teacher = teacher;
        }
    }

}