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

public class UpdateRouteFunctionalitySagas extends WorkflowFunctionality {
    private RouteDto updatedRouteDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateRouteFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, RouteDto routeDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(routeDto, unitOfWork);
    }

    public void buildWorkflow(RouteDto routeDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateRouteStep = new SagaStep("updateRouteStep", () -> {
            UpdateRouteCommand cmd = new UpdateRouteCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeDto);
            RouteDto updatedRouteDto = (RouteDto) commandGateway.send(cmd);
            setUpdatedRouteDto(updatedRouteDto);
        });

        workflow.addStep(updateRouteStep);
    }
    public RouteDto getUpdatedRouteDto() {
        return updatedRouteDto;
    }

    public void setUpdatedRouteDto(RouteDto updatedRouteDto) {
        this.updatedRouteDto = updatedRouteDto;
    }
}
