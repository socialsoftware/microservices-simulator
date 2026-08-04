package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class StationValidationAnnotations {

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

    public static class StayTimeValidation {
        @NotNull
        private Integer stayTime;
        
        public Integer getStayTime() {
            return stayTime;
        }
        
        public void setStayTime(Integer stayTime) {
            this.stayTime = stayTime;
        }
    }

}