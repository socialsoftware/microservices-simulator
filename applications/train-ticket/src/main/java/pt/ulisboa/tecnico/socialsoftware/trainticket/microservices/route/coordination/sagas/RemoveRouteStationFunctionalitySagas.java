package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.route.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveRouteStationFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveRouteStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer routeId, Integer stationAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(routeId, stationAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer routeId, Integer stationAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep removeStationStep = new SagaStep("removeStationStep", () -> {
            RemoveRouteStationCommand cmd = new RemoveRouteStationCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeId, stationAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(removeStationStep);
    }
}
