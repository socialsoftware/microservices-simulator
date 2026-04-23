package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PostBusinessRuleValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPostBusinessRule {
    String message() default "Post must comply with business rules";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}