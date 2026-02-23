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
import java.util.Set;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentCourse;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentTeacher;

public class EnrollmentValidationAnnotations {

    public static class CourseValidation {
        @NotNull
        private EnrollmentCourse course;
        
        public EnrollmentCourse getCourse() {
            return course;
        }
        
        public void setCourse(EnrollmentCourse course) {
            this.course = course;
        }
    }

    public static class TeachersValidation {
        @NotNull
    @NotEmpty
        private Set<EnrollmentTeacher> teachers;
        
        public Set<EnrollmentTeacher> getTeachers() {
            return teachers;
        }
        
        public void setTeachers(Set<EnrollmentTeacher> teachers) {
            this.teachers = teachers;
        }
    }

    public static class EnrollmentDateValidation {
        @NotNull
        private LocalDateTime enrollmentDate;
        
        public LocalDateTime getEnrollmentDate() {
            return enrollmentDate;
        }
        
        public void setEnrollmentDate(LocalDateTime enrollmentDate) {
            this.enrollmentDate = enrollmentDate;
        }
    }

    public static class ActiveValidation {
        @NotNull
        private Boolean active;
        
        public Boolean getActive() {
            return active;
        }
        
        public void setActive(Boolean active) {
            this.active = active;
        }
    }

}