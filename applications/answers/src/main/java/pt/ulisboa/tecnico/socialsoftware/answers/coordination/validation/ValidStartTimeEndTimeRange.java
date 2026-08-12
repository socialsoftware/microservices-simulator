package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = StartTimeEndTimeRangeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStartTimeEndTimeRange {
    String message() default "StartTime must be before EndTime";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}