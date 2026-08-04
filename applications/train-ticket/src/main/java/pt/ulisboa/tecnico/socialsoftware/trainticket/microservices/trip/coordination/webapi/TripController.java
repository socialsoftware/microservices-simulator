package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.functionalities.TripFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.webapi.requestDtos.CreateTripRequestDto;

@RestController
public class TripController {
    @Autowired
    private TripFunctionalities tripFunctionalities;

    @PostMapping("/trips/create")
    @ResponseStatus(HttpStatus.CREATED)
    public TripDto createTrip(@RequestBody CreateTripRequestDto createRequest) {
        return tripFunctionalities.createTrip(createRequest);
    }

    @GetMapping("/trips/{tripAggregateId}")
    public TripDto getTripById(@PathVariable Integer tripAggregateId) {
        return tripFunctionalities.getTripById(tripAggregateId);
    }

    @PutMapping("/trips")
    public TripDto updateTrip(@RequestBody TripDto tripDto) {
        return tripFunctionalities.updateTrip(tripDto);
    }

    @DeleteMapping("/trips/{tripAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable Integer tripAggregateId) {
        tripFunctionalities.deleteTrip(tripAggregateId);
    }

    @GetMapping("/trips")
    public List<TripDto> getAllTrips() {
        return tripFunctionalities.getAllTrips();
    }
}
