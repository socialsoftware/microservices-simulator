package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = CartBusinessRuleValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCartBusinessRule {
    String message() default "Cart must comply with business rules";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}