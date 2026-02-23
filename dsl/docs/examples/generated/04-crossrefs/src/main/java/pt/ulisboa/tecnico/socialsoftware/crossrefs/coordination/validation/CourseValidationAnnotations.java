package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.validation;


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