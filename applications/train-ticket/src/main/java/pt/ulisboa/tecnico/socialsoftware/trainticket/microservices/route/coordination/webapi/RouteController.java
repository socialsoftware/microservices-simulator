package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.functionalities.RouteFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.webapi.requestDtos.CreateRouteRequestDto;

@RestController
public class RouteController {
    @Autowired
    private RouteFunctionalities routeFunctionalities;

    @PostMapping("/routes/create")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteDto createRoute(@RequestBody CreateRouteRequestDto createRequest) {
        return routeFunctionalities.createRoute(createRequest);
    }

    @GetMapping("/routes/{routeAggregateId}")
    public RouteDto getRouteById(@PathVariable Integer routeAggregateId) {
        return routeFunctionalities.getRouteById(routeAggregateId);
    }

    @PutMapping("/routes")
    public RouteDto updateRoute(@RequestBody RouteDto routeDto) {
        return routeFunctionalities.updateRoute(routeDto);
    }

    @DeleteMapping("/routes/{routeAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable Integer routeAggregateId) {
        routeFunctionalities.deleteRoute(routeAggregateId);
    }

    @GetMapping("/routes")
    public List<RouteDto> getAllRoutes() {
        return routeFunctionalities.getAllRoutes();
    }

    @PostMapping("/routes/{routeId}/stations")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteStationDto addRouteStation(@PathVariable Integer routeId, @RequestParam Integer stationAggregateId, @RequestBody RouteStationDto stationDto) {
        return routeFunctionalities.addRouteStation(routeId, stationAggregateId, stationDto);
    }

    @PostMapping("/routes/{routeId}/stations/batch")
    public List<RouteStationDto> addRouteStations(@PathVariable Integer routeId, @RequestBody List<RouteStationDto> stationDtos) {
        return routeFunctionalities.addRouteStations(routeId, stationDtos);
    }

    @GetMapping("/routes/{routeId}/stations/{stationAggregateId}")
    public RouteStationDto getRouteStation(@PathVariable Integer routeId, @PathVariable Integer stationAggregateId) {
        return routeFunctionalities.getRouteStation(routeId, stationAggregateId);
    }

    @PutMapping("/routes/{routeId}/stations/{stationAggregateId}")
    public RouteStationDto updateRouteStation(@PathVariable Integer routeId, @PathVariable Integer stationAggregateId, @RequestBody RouteStationDto stationDto) {
        return routeFunctionalities.updateRouteStation(routeId, stationAggregateId, stationDto);
    }

    @DeleteMapping("/routes/{routeId}/stations/{stationAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRouteStation(@PathVariable Integer routeId, @PathVariable Integer stationAggregateId) {
        routeFunctionalities.removeRouteStation(routeId, stationAggregateId);
    }
}
