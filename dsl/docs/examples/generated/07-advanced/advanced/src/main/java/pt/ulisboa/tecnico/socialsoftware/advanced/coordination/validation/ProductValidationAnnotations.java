package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

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

    public static class AvailableValidation {
        @NotNull
        private Boolean available;
        
        public Boolean getAvailable() {
            return available;
        }
        
        public void setAvailable(Boolean available) {
            this.available = available;
        }
    }

}