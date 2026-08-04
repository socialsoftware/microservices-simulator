package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class TrainValidationAnnotations {

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

    public static class EconomyClassValidation {
        @NotNull
        private Integer economyClass;
        
        public Integer getEconomyClass() {
            return economyClass;
        }
        
        public void setEconomyClass(Integer economyClass) {
            this.economyClass = economyClass;
        }
    }

    public static class ConfortClassValidation {
        @NotNull
        private Integer confortClass;
        
        public Integer getConfortClass() {
            return confortClass;
        }
        
        public void setConfortClass(Integer confortClass) {
            this.confortClass = confortClass;
        }
    }

    public static class AverageSpeedValidation {
        @NotNull
        private Integer averageSpeed;
        
        public Integer getAverageSpeed() {
            return averageSpeed;
        }
        
        public void setAverageSpeed(Integer averageSpeed) {
            this.averageSpeed = averageSpeed;
        }
    }

}