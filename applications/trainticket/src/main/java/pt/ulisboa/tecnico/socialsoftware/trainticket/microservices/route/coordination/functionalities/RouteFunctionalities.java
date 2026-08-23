package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.GetRouteByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.GetRoutesByStationFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.GetRoutesFunctionalitySagas;

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
}
