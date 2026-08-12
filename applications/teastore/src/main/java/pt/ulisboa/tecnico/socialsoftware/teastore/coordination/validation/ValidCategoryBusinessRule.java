package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = CategoryBusinessRuleValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCategoryBusinessRule {
    String message() default "Category must comply with business rules";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}