package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateTrainRequestDto {
    @NotNull
    private String name;
    @NotNull
    private Integer economyClass;
    @NotNull
    private Integer confortClass;
    @NotNull
    private Integer averageSpeed;

    public CreateTrainRequestDto() {}

    public CreateTrainRequestDto(String name, Integer economyClass, Integer confortClass, Integer averageSpeed) {
        this.name = name;
        this.economyClass = economyClass;
        this.confortClass = confortClass;
        this.averageSpeed = averageSpeed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Integer getEconomyClass() {
        return economyClass;
    }

    public void setEconomyClass(Integer economyClass) {
        this.economyClass = economyClass;
    }
    public Integer getConfortClass() {
        return confortClass;
    }

    public void setConfortClass(Integer confortClass) {
        this.confortClass = confortClass;
    }
    public Integer getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Integer averageSpeed) {
        this.averageSpeed = averageSpeed;
    }
}
