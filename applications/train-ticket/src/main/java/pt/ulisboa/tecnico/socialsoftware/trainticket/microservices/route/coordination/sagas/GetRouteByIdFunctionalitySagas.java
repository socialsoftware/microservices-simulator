package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.route.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetRouteByIdFunctionalitySagas extends WorkflowFunctionality {
    private RouteDto routeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetRouteByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer routeAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(routeAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer routeAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getRouteStep = new SagaStep("getRouteStep", () -> {
            GetRouteByIdCommand cmd = new GetRouteByIdCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeAggregateId);
            RouteDto routeDto = (RouteDto) commandGateway.send(cmd);
            setRouteDto(routeDto);
        });

        workflow.addStep(getRouteStep);
    }
    public RouteDto getRouteDto() {
        return routeDto;
    }

    public void setRouteDto(RouteDto routeDto) {
        this.routeDto = routeDto;
    }
}
