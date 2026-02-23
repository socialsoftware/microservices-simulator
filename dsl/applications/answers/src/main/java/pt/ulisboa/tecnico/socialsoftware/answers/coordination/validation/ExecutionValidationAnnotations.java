package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;

public class ExecutionValidationAnnotations {

    public static class AcronymValidation {
        @NotNull
    @NotBlank
        private String acronym;
        
        public String getAcronym() {
            return acronym;
        }
        
        public void setAcronym(String acronym) {
            this.acronym = acronym;
        }
    }

    public static class AcademicTermValidation {
        @NotNull
    @NotBlank
        private String academicTerm;
        
        public String getAcademicTerm() {
            return academicTerm;
        }
        
        public void setAcademicTerm(String academicTerm) {
            this.academicTerm = academicTerm;
        }
    }

    public static class EndDateValidation {
        @NotNull
        private LocalDateTime endDate;
        
        public LocalDateTime getEndDate() {
            return endDate;
        }
        
        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
        }
    }

    public static class CourseValidation {
        @NotNull
        private ExecutionCourse course;
        
        public ExecutionCourse getCourse() {
            return course;
        }
        
        public void setCourse(ExecutionCourse course) {
            this.course = course;
        }
    }

    public static class UsersValidation {
        @NotNull
    @NotEmpty
        private Set<ExecutionUser> users;
        
        public Set<ExecutionUser> getUsers() {
            return users;
        }
        
        public void setUsers(Set<ExecutionUser> users) {
            this.users = users;
        }
    }

}