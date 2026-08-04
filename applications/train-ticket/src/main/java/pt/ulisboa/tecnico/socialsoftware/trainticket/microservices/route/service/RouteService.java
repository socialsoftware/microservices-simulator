package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteStationRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteStationUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.webapi.requestDtos.CreateRouteRequestDto;


@Service
@Transactional
public class RouteService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteFactory routeFactory;

    public RouteService() {}

    public RouteDto createRoute(CreateRouteRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            RouteDto routeDto = new RouteDto();
            if (createRequest.getStations() != null) {
                routeDto.setStations(createRequest.getStations().stream().map(srcDto -> {
                    RouteStationDto projDto = new RouteStationDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState() != null ? srcDto.getState().name() : null);
                    return projDto;
                }).collect(Collectors.toList()));
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Route route = routeFactory.createRoute(aggregateId, routeDto);
            unitOfWorkService.registerChanged(route, unitOfWork);
            return routeFactory.createRouteDto(route);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating route: " + e.getMessage());
        }
    }

    public RouteDto getRouteById(Integer id, UnitOfWork unitOfWork) {
        try {
            Route route = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return routeFactory.createRouteDto(route);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving route: " + e.getMessage());
        }
    }

    public List<RouteDto> getAllRoutes(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = routeRepository.findAll().stream()
                .map(Route::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Route) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(routeFactory::createRouteDto)
                .collect(Collectors.toList());
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving route: " + e.getMessage());
        }
    }

    public RouteDto updateRoute(RouteDto routeDto, UnitOfWork unitOfWork) {
        try {
            Integer id = routeDto.getAggregateId();
            Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Route newRoute = routeFactory.createRouteFromExisting(oldRoute);


            unitOfWorkService.registerChanged(newRoute, unitOfWork);            RouteUpdatedEvent event = new RouteUpdatedEvent(newRoute.getAggregateId());
            event.setPublisherAggregateVersion(newRoute.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return routeFactory.createRouteDto(newRoute);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating route: " + e.getMessage());
        }
    }

    public void deleteRoute(Integer id, UnitOfWork unitOfWork) {
        try {
            Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Route newRoute = routeFactory.createRouteFromExisting(oldRoute);
            newRoute.remove();
            unitOfWorkService.registerChanged(newRoute, unitOfWork);            unitOfWorkService.registerEvent(new RouteDeletedEvent(newRoute.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting route: " + e.getMessage());
        }
    }

    public RouteStationDto addRouteStation(Integer routeId, Integer stationAggregateId, RouteStationDto RouteStationDto, UnitOfWork unitOfWork) {
        try {
            Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeId, unitOfWork);
            Route newRoute = routeFactory.createRouteFromExisting(oldRoute);
            RouteStation element = new RouteStation(RouteStationDto);
            newRoute.getStations().add(element);
            unitOfWorkService.registerChanged(newRoute, unitOfWork);
            return RouteStationDto;
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error adding RouteStation: " + e.getMessage());
        }
    }

    public List<RouteStationDto> addRouteStations(Integer routeId, List<RouteStationDto> RouteStationDtos, UnitOfWork unitOfWork) {
        try {
            Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeId, unitOfWork);
            Route newRoute = routeFactory.createRouteFromExisting(oldRoute);
            RouteStationDtos.forEach(dto -> {
                RouteStation element = new RouteStation(dto);
                newRoute.getStations().add(element);
            });
            unitOfWorkService.registerChanged(newRoute, unitOfWork);
            return RouteStationDtos;
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error adding RouteStations: " + e.getMessage());
        }
    }

    public RouteStationDto getRouteStation(Integer routeId, Integer stationAggregateId, UnitOfWork unitOfWork) {
        try {
            Route route = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeId, unitOfWork);
            RouteStation element = route.getStations().stream()
                .filter(item -> item.getStationAggregateId() != null &&
                               item.getStationAggregateId().equals(stationAggregateId))
                .findFirst()
                .orElseThrow(() -> new TrainTicketException("RouteStation not found"));
            return element.buildDto();
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving RouteStation: " + e.getMessage());
        }
    }

    public void removeRouteStation(Integer routeId, Integer stationAggregateId, UnitOfWork unitOfWork) {
        try {
            Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeId, unitOfWork);
            Route newRoute = routeFactory.createRouteFromExisting(oldRoute);
            newRoute.getStations().removeIf(item ->
                item.getStationAggregateId() != null &&
                item.getStationAggregateId().equals(stationAggregateId)
            );
            unitOfWorkService.registerChanged(newRoute, unitOfWork);
            RouteStationRemovedEvent event = new RouteStationRemovedEvent(routeId, stationAggregateId);
            event.setPublisherAggregateVersion(newRoute.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error removing RouteStation: " + e.getMessage());
        }
    }

    public RouteStationDto updateRouteStation(Integer routeId, Integer stationAggregateId, RouteStationDto RouteStationDto, UnitOfWork unitOfWork) {
        try {
            Route oldRoute = (Route) unitOfWorkService.aggregateLoadAndRegisterRead(routeId, unitOfWork);
            Route newRoute = routeFactory.createRouteFromExisting(oldRoute);
            RouteStation element = newRoute.getStations().stream()
                .filter(item -> item.getStationAggregateId() != null &&
                               item.getStationAggregateId().equals(stationAggregateId))
                .findFirst()
                .orElseThrow(() -> new TrainTicketException("RouteStation not found"));

            unitOfWorkService.registerChanged(newRoute, unitOfWork);
            RouteStationUpdatedEvent event = new RouteStationUpdatedEvent(routeId, element.getStationAggregateId(), element.getStationVersion(), element.getStationName(), element.getStationOrder(), element.getDistanceFromStart());
            event.setPublisherAggregateVersion(newRoute.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating RouteStation: " + e.getMessage());
        }
    }






}