package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class ProductValidationAnnotations {

    public static class SkuValidation {
        @NotNull
    @NotBlank
        private String sku;
        
        public String getSku() {
            return sku;
        }
        
        public void setSku(String sku) {
            this.sku = sku;
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

    public static class PriceInCentsValidation {
        @NotNull
        private Double priceInCents;
        
        public Double getPriceInCents() {
            return priceInCents;
        }
        
        public void setPriceInCents(Double priceInCents) {
            this.priceInCents = priceInCents;
        }
    }

    public static class StockValidation {
        @NotNull
        private Integer stock;
        
        public Integer getStock() {
            return stock;
        }
        
        public void setStock(Integer stock) {
            this.stock = stock;
        }
    }

}