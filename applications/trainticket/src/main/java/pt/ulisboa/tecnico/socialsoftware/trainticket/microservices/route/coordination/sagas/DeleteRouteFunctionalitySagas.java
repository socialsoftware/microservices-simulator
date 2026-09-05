package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.DeleteRouteCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRouteByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.states.RouteSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteRouteFunctionalitySagas extends WorkflowFunctionality {
    private RouteDto routeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteRouteFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         Integer routeAggregateId,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(routeAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer routeAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getRouteStep = new SagaStep("getRouteStep", () -> {
            GetRouteByIdCommand readCmd = new GetRouteByIdCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(RouteSagaState.IN_DELETE_ROUTE);
            this.routeDto = (RouteDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteRouteStep = new SagaStep("deleteRouteStep", () -> {
            DeleteRouteCommand cmd = new DeleteRouteCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getRouteStep)));

        this.workflow.addStep(getRouteStep);
        this.workflow.addStep(deleteRouteStep);
    }

    public RouteDto getRouteDto() {
        return routeDto;
    }
}
