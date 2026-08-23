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

import java.util.ArrayList;
import java.util.List;

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

    private List<RouteDto> toDtos(List<Route> routes, UnitOfWork unitOfWork) {
        List<RouteDto> routeDtos = new ArrayList<>();
        for (Route route : routes) {
            routeDtos.add(routeFactory.createRouteDto(
                    (Route) unitOfWorkService.aggregateLoadAndRegisterRead(route.getAggregateId(), unitOfWork)));
        }
        return routeDtos;
    }
}
