package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRouteByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;

public class GetRouteByIdFunctionalitySagas extends WorkflowFunctionality {
    private RouteDto routeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetRouteByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          Integer routeAggregateId,
                                          SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(routeAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer routeAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getRouteStep = new SagaStep("getRouteStep", () -> {
            GetRouteByIdCommand cmd = new GetRouteByIdCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeAggregateId);
            this.routeDto = (RouteDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getRouteStep);
    }

    public RouteDto getRouteDto() {
        return routeDto;
    }
}
