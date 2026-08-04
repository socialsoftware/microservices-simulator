package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities.StationFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.webapi.requestDtos.CreateStationRequestDto;

@RestController
public class StationController {
    @Autowired
    private StationFunctionalities stationFunctionalities;

    @PostMapping("/stations/create")
    @ResponseStatus(HttpStatus.CREATED)
    public StationDto createStation(@RequestBody CreateStationRequestDto createRequest) {
        return stationFunctionalities.createStation(createRequest);
    }

    @GetMapping("/stations/{stationAggregateId}")
    public StationDto getStationById(@PathVariable Integer stationAggregateId) {
        return stationFunctionalities.getStationById(stationAggregateId);
    }

    @PutMapping("/stations")
    public StationDto updateStation(@RequestBody StationDto stationDto) {
        return stationFunctionalities.updateStation(stationDto);
    }

    @DeleteMapping("/stations/{stationAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStation(@PathVariable Integer stationAggregateId) {
        stationFunctionalities.deleteStation(stationAggregateId);
    }

    @GetMapping("/stations")
    public List<StationDto> getAllStations() {
        return stationFunctionalities.getAllStations();
    }
}
