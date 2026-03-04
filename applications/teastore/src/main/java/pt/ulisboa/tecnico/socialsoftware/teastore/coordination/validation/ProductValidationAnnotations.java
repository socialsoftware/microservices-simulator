package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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