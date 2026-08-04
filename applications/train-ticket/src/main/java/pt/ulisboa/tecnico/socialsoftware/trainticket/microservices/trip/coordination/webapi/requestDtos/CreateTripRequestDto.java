package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.TripType;

public class CreateTripRequestDto {
    @NotNull
    private TrainDto trainType;
    @NotNull
    private RouteDto route;
    @NotNull
    private StationDto startStation;
    @NotNull
    private StationDto terminalStation;
    @NotNull
    private TripType tripType;
    @NotNull
    private String tripNumber;
    @NotNull
    private String startTime;
    @NotNull
    private String endTime;

    public CreateTripRequestDto() {}

    public CreateTripRequestDto(TrainDto trainType, RouteDto route, StationDto startStation, StationDto terminalStation, TripType tripType, String tripNumber, String startTime, String endTime) {
        this.trainType = trainType;
        this.route = route;
        this.startStation = startStation;
        this.terminalStation = terminalStation;
        this.tripType = tripType;
        this.tripNumber = tripNumber;
        this.startTime = startTime;
        this.endTime = endTime;
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
    public StationDto getStartStation() {
        return startStation;
    }

    public void setStartStation(StationDto startStation) {
        this.startStation = startStation;
    }
    public StationDto getTerminalStation() {
        return terminalStation;
    }

    public void setTerminalStation(StationDto terminalStation) {
        this.terminalStation = terminalStation;
    }
    public TripType getTripType() {
        return tripType;
    }

    public void setTripType(TripType tripType) {
        this.tripType = tripType;
    }
    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
