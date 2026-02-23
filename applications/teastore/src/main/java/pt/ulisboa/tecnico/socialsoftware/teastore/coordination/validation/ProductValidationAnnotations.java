package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductCategory;

public class ProductValidationAnnotations {

    public static class ProductCategoryValidation {
        @NotNull
        private ProductCategory productCategory;
        
        public ProductCategory getProductCategory() {
            return productCategory;
        }
        
        public void setProductCategory(ProductCategory productCategory) {
            this.productCategory = productCategory;
        }
    }

    public static class NameValidation {
        @NotNull
    @NotBlank
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
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

    public static class ListPriceInCentsValidation {
        @NotNull
        private Double listPriceInCents;
        
        public Double getListPriceInCents() {
            return listPriceInCents;
        }
        
        public void setListPriceInCents(Double listPriceInCents) {
            this.listPriceInCents = listPriceInCents;
        }
    }

}