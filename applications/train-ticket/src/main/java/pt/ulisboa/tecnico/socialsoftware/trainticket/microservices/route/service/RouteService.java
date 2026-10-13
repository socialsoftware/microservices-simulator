package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service;

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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteService {
    private final RouteCustomRepository routeCustomRepository;
    private final RouteFactory routeFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public RouteService(RouteCustomRepository routeCustomRepository,
                        RouteFactory routeFactory,
                        UnitOfWorkService unitOfWorkService,
                        AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.routeCustomRepository = routeCustomRepository;
        this.routeFactory = routeFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RouteDto createRoute(List<RouteStationDto> stationDtos, UnitOfWork unitOfWork) {
        List<RouteStation> stations = stationDtos.stream()
                .map(RouteStation::new)
                .collect(Collectors.toList());

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Route route = routeFactory.createRoute(aggregateId, stations);

        unitOfWorkService.registerChanged(route, unitOfWork);
        return routeFactory.createRouteDto(route);
    }
}
