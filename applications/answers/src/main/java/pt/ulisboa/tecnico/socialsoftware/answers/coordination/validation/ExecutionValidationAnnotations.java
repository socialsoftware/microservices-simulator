package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.validation.annotations;

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

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;


/**
 * Validation annotations for Execution properties
 */
public class ExecutionValidationAnnotations {

    /**
     * Validation annotations for acronym
     */
    public static class AcronymValidation {
        @NotNull
    @NotBlank
        private String acronym;
        
        // Getter and setter
        public String getAcronym() {
            return acronym;
        }
        
        public void setAcronym(String acronym) {
            this.acronym = acronym;
        }
    }

    /**
     * Validation annotations for academicTerm
     */
    public static class AcademicTermValidation {
        @NotNull
    @NotBlank
        private String academicTerm;
        
        // Getter and setter
        public String getAcademicTerm() {
            return academicTerm;
        }
        
        public void setAcademicTerm(String academicTerm) {
            this.academicTerm = academicTerm;
        }
    }

    /**
     * Validation annotations for endDate
     */
    public static class EndDateValidation {
        @NotNull
        private LocalDateTime endDate;
        
        // Getter and setter
        public LocalDateTime getEndDate() {
            return endDate;
        }
        
        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
        }
    }

    /**
     * Validation annotations for executionCourse
     */
    public static class ExecutionCourseValidation {
        @NotNull
        private ExecutionCourse executionCourse;
        
        // Getter and setter
        public ExecutionCourse getExecutionCourse() {
            return executionCourse;
        }
        
        public void setExecutionCourse(ExecutionCourse executionCourse) {
            this.executionCourse = executionCourse;
        }
    }

    /**
     * Validation annotations for students
     */
    public static class StudentsValidation {
        @NotNull
    @NotEmpty
        private Set<ExecutionStudent> students;
        
        // Getter and setter
        public Set<ExecutionStudent> getStudents() {
            return students;
        }
        
        public void setStudents(Set<ExecutionStudent> students) {
            this.students = students;
        }
    }

}