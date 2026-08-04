package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRoute;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripStartStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripTerminalStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripTrain;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.TripType;

public class TripValidationAnnotations {

    public static class TripTypeValidation {
        @NotNull
        private TripType tripType;
        
        public TripType getTripType() {
            return tripType;
        }
        
        public void setTripType(TripType tripType) {
            this.tripType = tripType;
        }
    }

    public static class TripNumberValidation {
        @NotNull
    @NotBlank
        private String tripNumber;
        
        public String getTripNumber() {
            return tripNumber;
        }
        
        public void setTripNumber(String tripNumber) {
            this.tripNumber = tripNumber;
        }
    }

    public static class StartTimeValidation {
        @NotNull
    @NotBlank
        private String startTime;
        
        public String getStartTime() {
            return startTime;
        }
        
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
    }

    public static class EndTimeValidation {
        @NotNull
    @NotBlank
        private String endTime;
        
        public String getEndTime() {
            return endTime;
        }
        
        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }

    public static class TrainTypeValidation {
        @NotNull
        private TripTrain trainType;
        
        public TripTrain getTrainType() {
            return trainType;
        }
        
        public void setTrainType(TripTrain trainType) {
            this.trainType = trainType;
        }
    }

    public static class RouteValidation {
        @NotNull
        private TripRoute route;
        
        public TripRoute getRoute() {
            return route;
        }
        
        public void setRoute(TripRoute route) {
            this.route = route;
        }
    }

    public static class StartStationValidation {
        @NotNull
        private TripStartStation startStation;
        
        public TripStartStation getStartStation() {
            return startStation;
        }
        
        public void setStartStation(TripStartStation startStation) {
            this.startStation = startStation;
        }
    }

    public static class TerminalStationValidation {
        @NotNull
        private TripTerminalStation terminalStation;
        
        public TripTerminalStation getTerminalStation() {
            return terminalStation;
        }
        
        public void setTerminalStation(TripTerminalStation terminalStation) {
            this.terminalStation = terminalStation;
        }
    }

}