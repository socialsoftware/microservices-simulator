package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;

public class CreatePriceConfigRequestDto {
    @NotNull
    private TrainDto trainType;
    @NotNull
    private RouteDto route;
    @NotNull
    private Double basicPriceRate;
    @NotNull
    private Double firstClassPriceRate;

    public CreatePriceConfigRequestDto() {}

    public CreatePriceConfigRequestDto(TrainDto trainType, RouteDto route, Double basicPriceRate, Double firstClassPriceRate) {
        this.trainType = trainType;
        this.route = route;
        this.basicPriceRate = basicPriceRate;
        this.firstClassPriceRate = firstClassPriceRate;
    }

    public TrainDto getTrainType() {
        return trainType;
    }

    public void setTrainType(TrainDto trainType) {
        this.trainType = trainType;
    }
    public RouteDto getRoute() {
        return route;
    }

    public void setRoute(RouteDto route) {
        this.route = route;
    }
    public Double getBasicPriceRate() {
        return basicPriceRate;
    }

    public void setBasicPriceRate(Double basicPriceRate) {
        this.basicPriceRate = basicPriceRate;
    }
    public Double getFirstClassPriceRate() {
        return firstClassPriceRate;
    }

    public void setFirstClassPriceRate(Double firstClassPriceRate) {
        this.firstClassPriceRate = firstClassPriceRate;
    }
}
