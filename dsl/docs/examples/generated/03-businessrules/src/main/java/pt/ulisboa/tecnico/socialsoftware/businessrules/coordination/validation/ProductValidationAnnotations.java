package pt.ulisboa.tecnico.socialsoftware.businessrules.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class ProductValidationAnnotations {

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

    public static class PriceValidation {
        @NotNull
        private Double price;
        
        public Double getPrice() {
            return price;
        }
        
        public void setPrice(Double price) {
            this.price = price;
        }
    }

    public static class StockQuantityValidation {
        @NotNull
        private Integer stockQuantity;
        
        public Integer getStockQuantity() {
            return stockQuantity;
        }
        
        public void setStockQuantity(Integer stockQuantity) {
            this.stockQuantity = stockQuantity;
        }
    }

    public static class ActiveValidation {
        @NotNull
        private Boolean active;
        
        public Boolean getActive() {
            return active;
        }
        
        public void setActive(Boolean active) {
            this.active = active;
        }
    }

}