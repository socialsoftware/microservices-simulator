package com.generated.microservices.answers.microservices.course.validation.annotations;

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

import com.generated.microservices.answers.microservices.course.aggregate.*;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


/**
 * Validation annotations for Course properties
 */
public class CourseValidationAnnotations {

    /**
     * Validation annotations for name
     */
    public static class NameValidation {
        @NotNull
    @NotBlank
        private String name;
        
        // Getter and setter
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }

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
     * Validation annotations for courseType
     */
    public static class CourseTypeValidation {
        @NotNull
    @NotBlank
        private String courseType;
        
        // Getter and setter
        public String getCourseType() {
            return courseType;
        }
        
        public void setCourseType(String courseType) {
            this.courseType = courseType;
        }
    }

    /**
     * Validation annotations for creationDate
     */
    public static class CreationDateValidation {
        @NotNull
        private LocalDateTime creationDate;
        
        // Getter and setter
        public LocalDateTime getCreationDate() {
            return creationDate;
        }
        
        public void setCreationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
        }
    }

}