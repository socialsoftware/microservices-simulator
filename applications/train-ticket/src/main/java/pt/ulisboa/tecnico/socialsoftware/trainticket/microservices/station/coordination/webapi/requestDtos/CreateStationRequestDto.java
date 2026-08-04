package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateStationRequestDto {
    @NotNull
    private String name;
    @NotNull
    private Integer stayTime;

    public CreateStationRequestDto() {}

    public CreateStationRequestDto(String name, Integer stayTime) {
        this.name = name;
        this.stayTime = stayTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Integer getStayTime() {
        return stayTime;
    }

    public void setStayTime(Integer stayTime) {
        this.stayTime = stayTime;
    }
}
