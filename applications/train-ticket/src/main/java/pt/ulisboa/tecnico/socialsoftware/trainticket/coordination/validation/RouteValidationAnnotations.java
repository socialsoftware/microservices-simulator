package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;

public class RouteValidationAnnotations {

    public static class StationsValidation {
        @NotNull
    @NotEmpty
        private List<RouteStation> stations;
        
        public List<RouteStation> getStations() {
            return stations;
        }
        
        public void setStations(List<RouteStation> stations) {
            this.stations = stations;
        }
    }

}