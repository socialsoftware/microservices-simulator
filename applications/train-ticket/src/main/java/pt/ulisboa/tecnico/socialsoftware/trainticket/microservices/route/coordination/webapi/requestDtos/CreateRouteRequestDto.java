package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import java.util.Set;

public class CreateRouteRequestDto {
    @NotNull
    private Set<StationDto> stations;

    public CreateRouteRequestDto() {}

    public CreateRouteRequestDto(Set<StationDto> stations) {
        this.stations = stations;
    }

    public Set<StationDto> getStations() {
        return stations;
    }

    public void setStations(Set<StationDto> stations) {
        this.stations = stations;
    }
}
