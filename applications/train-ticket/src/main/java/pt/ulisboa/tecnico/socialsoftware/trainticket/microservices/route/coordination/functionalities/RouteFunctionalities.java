package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service.RouteService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.webapi.requestDtos.CreateRouteRequestDto;
import java.util.List;

@Service
public class RouteFunctionalities {
    @Autowired
    private RouteService routeService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RouteDto createRoute(CreateRouteRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateRouteFunctionalitySagas createRouteFunctionalitySagas = new CreateRouteFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createRouteFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createRouteFunctionalitySagas.getCreatedRouteDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RouteDto getRouteById(Integer routeAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetRouteByIdFunctionalitySagas getRouteByIdFunctionalitySagas = new GetRouteByIdFunctionalitySagas(
                        sagaUnitOfWorkService, routeAggregateId, sagaUnitOfWork, commandGateway);
                getRouteByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getRouteByIdFunctionalitySagas.getRouteDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RouteDto updateRoute(RouteDto routeDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(routeDto);
                UpdateRouteFunctionalitySagas updateRouteFunctionalitySagas = new UpdateRouteFunctionalitySagas(
                        sagaUnitOfWorkService, routeDto, sagaUnitOfWork, commandGateway);
                updateRouteFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateRouteFunctionalitySagas.getUpdatedRouteDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteRoute(Integer routeAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteRouteFunctionalitySagas deleteRouteFunctionalitySagas = new DeleteRouteFunctionalitySagas(
                        sagaUnitOfWorkService, routeAggregateId, sagaUnitOfWork, commandGateway);
                deleteRouteFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<RouteDto> getAllRoutes() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllRoutesFunctionalitySagas getAllRoutesFunctionalitySagas = new GetAllRoutesFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllRoutesFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllRoutesFunctionalitySagas.getRoutes();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RouteStationDto addRouteStation(Integer routeId, Integer stationAggregateId, RouteStationDto stationDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddRouteStationFunctionalitySagas addRouteStationFunctionalitySagas = new AddRouteStationFunctionalitySagas(
                        sagaUnitOfWorkService,
                        routeId, stationAggregateId, stationDto,
                        sagaUnitOfWork, commandGateway);
                addRouteStationFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addRouteStationFunctionalitySagas.getAddedStationDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<RouteStationDto> addRouteStations(Integer routeId, List<RouteStationDto> stationDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddRouteStationsFunctionalitySagas addRouteStationsFunctionalitySagas = new AddRouteStationsFunctionalitySagas(
                        sagaUnitOfWorkService,
                        routeId, stationDtos,
                        sagaUnitOfWork, commandGateway);
                addRouteStationsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addRouteStationsFunctionalitySagas.getAddedStationDtos();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RouteStationDto getRouteStation(Integer routeId, Integer stationAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetRouteStationFunctionalitySagas getRouteStationFunctionalitySagas = new GetRouteStationFunctionalitySagas(
                        sagaUnitOfWorkService,
                        routeId, stationAggregateId,
                        sagaUnitOfWork, commandGateway);
                getRouteStationFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getRouteStationFunctionalitySagas.getStationDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RouteStationDto updateRouteStation(Integer routeId, Integer stationAggregateId, RouteStationDto stationDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateRouteStationFunctionalitySagas updateRouteStationFunctionalitySagas = new UpdateRouteStationFunctionalitySagas(
                        sagaUnitOfWorkService,
                        routeId, stationAggregateId, stationDto,
                        sagaUnitOfWork, commandGateway);
                updateRouteStationFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateRouteStationFunctionalitySagas.getUpdatedStationDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeRouteStation(Integer routeId, Integer stationAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveRouteStationFunctionalitySagas removeRouteStationFunctionalitySagas = new RemoveRouteStationFunctionalitySagas(
                        sagaUnitOfWorkService,
                        routeId, stationAggregateId,
                        sagaUnitOfWork, commandGateway);
                removeRouteStationFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(RouteDto routeDto) {
}

    private void checkInput(CreateRouteRequestDto createRequest) {
}
}