package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RouteService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private RouteFactory routeFactory;

    private final RouteRepository routeRepository;
    private final RouteCustomRepository routeCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public RouteService(UnitOfWorkService unitOfWorkService,
                        RouteRepository routeRepository,
                        RouteCustomRepository routeCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.routeRepository = routeRepository;
        this.routeCustomRepository = routeCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RouteDto getRouteById(Integer routeAggregateId, UnitOfWork unitOfWork) {
        return routeFactory.createRouteDto(
                (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<RouteDto> getRoutes(UnitOfWork unitOfWork) {
        return toDtos(routeCustomRepository.findAllLatestActive(), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<RouteDto> getRoutesByStation(Integer stationAggregateId, UnitOfWork unitOfWork) {
        return toDtos(routeCustomRepository.findAllLatestActiveByStation(stationAggregateId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RouteDto createRoute(RouteDto routeDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Route route = routeFactory.createRoute(aggregateId, routeDto);

        unitOfWorkService.registerChanged(route, unitOfWork);
        return routeFactory.createRouteDto(route);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateRoute(Integer routeAggregateId, RouteDto routeDto, UnitOfWork unitOfWork) {
        Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeAggregateId, unitOfWork);
        Route newRoute = routeFactory.createRouteCopy(oldRoute);
        newRoute.setStartStationName(routeDto.getStartStationName());
        newRoute.setEndStationName(routeDto.getEndStationName());
        newRoute.setRouteStations(toRouteStations(routeDto));

        unitOfWorkService.registerChanged(newRoute, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteRoute(Integer routeAggregateId, UnitOfWork unitOfWork) {
        Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeAggregateId, unitOfWork);
        Route newRoute = routeFactory.createRouteCopy(oldRoute);
        newRoute.remove();

        unitOfWorkService.registerChanged(newRoute, unitOfWork);
    }

    private Set<RouteStation> toRouteStations(RouteDto routeDto) {
        return routeDto.getRouteStations().stream()
                .map(RouteStation::new)
                .collect(Collectors.toSet());
    }

    private List<RouteDto> toDtos(List<Route> routes, UnitOfWork unitOfWork) {
        List<RouteDto> routeDtos = new ArrayList<>();
        for (Route route : routes) {
            routeDtos.add(routeFactory.createRouteDto(
                    (Route) unitOfWorkService.aggregateLoadAndRegisterRead(route.getAggregateId(), unitOfWork)));
        }
        return routeDtos;
    }
}
