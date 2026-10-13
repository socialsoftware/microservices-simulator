package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.CreateRouteFunctionalitySagas;

import java.util.List;

@Service
public class RouteFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

    public RouteDto createRoute(List<RouteStationDto> stops) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createRoute");
        CreateRouteFunctionalitySagas saga = new CreateRouteFunctionalitySagas(
                unitOfWorkService, stops, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getRouteDto();
    }
}
