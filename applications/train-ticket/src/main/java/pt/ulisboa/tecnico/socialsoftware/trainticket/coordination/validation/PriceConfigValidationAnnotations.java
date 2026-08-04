package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRoute;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigTrain;

public class PriceConfigValidationAnnotations {

    public static class BasicPriceRateValidation {
        @NotNull
        private Double basicPriceRate;
        
        public Double getBasicPriceRate() {
            return basicPriceRate;
        }
        
        public void setBasicPriceRate(Double basicPriceRate) {
            this.basicPriceRate = basicPriceRate;
        }
    }

    public static class FirstClassPriceRateValidation {
        @NotNull
        private Double firstClassPriceRate;
        
        public Double getFirstClassPriceRate() {
            return firstClassPriceRate;
        }
        
        public void setFirstClassPriceRate(Double firstClassPriceRate) {
            this.firstClassPriceRate = firstClassPriceRate;
        }
    }

    public static class TrainTypeValidation {
        @NotNull
        private PriceConfigTrain trainType;
        
        public PriceConfigTrain getTrainType() {
            return trainType;
        }
        
        public void setTrainType(PriceConfigTrain trainType) {
            this.trainType = trainType;
        }
    }

    public static class RouteValidation {
        @NotNull
        private PriceConfigRoute route;
        
        public PriceConfigRoute getRoute() {
            return route;
        }
        
        public void setRoute(PriceConfigRoute route) {
            this.route = route;
        }
    }

}