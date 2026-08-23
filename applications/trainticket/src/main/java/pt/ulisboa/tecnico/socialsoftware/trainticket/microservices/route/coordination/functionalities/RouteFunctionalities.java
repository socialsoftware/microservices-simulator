package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.CreateRouteFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.DeleteRouteFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.GetRouteByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.GetRoutesByStationFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.GetRoutesFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.UpdateRouteFunctionalitySagas;

import java.util.List;

@Service
public class RouteFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public RouteDto getRouteById(Integer routeAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getRouteById");
        GetRouteByIdFunctionalitySagas saga = new GetRouteByIdFunctionalitySagas(
                unitOfWorkService, routeAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getRouteDto();
    }

    public List<RouteDto> getRoutes() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getRoutes");
        GetRoutesFunctionalitySagas saga = new GetRoutesFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getRoutes();
    }

    public List<RouteDto> getRoutesByStation(Integer stationAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getRoutesByStation");
        GetRoutesByStationFunctionalitySagas saga = new GetRoutesByStationFunctionalitySagas(
                unitOfWorkService, stationAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getRoutes();
    }

    public RouteDto createRoute(RouteDto routeDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateRouteFunctionalitySagas saga = new CreateRouteFunctionalitySagas(
                unitOfWorkService, routeDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedRouteDto();
    }

    public void updateRoute(Integer routeAggregateId, RouteDto routeDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateRouteFunctionalitySagas saga = new UpdateRouteFunctionalitySagas(
                unitOfWorkService, routeAggregateId, routeDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteRoute(Integer routeAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteRouteFunctionalitySagas saga = new DeleteRouteFunctionalitySagas(
                unitOfWorkService, routeAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
