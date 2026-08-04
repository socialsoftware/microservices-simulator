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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.webapi.requestDtos.CreateRouteRequestDto;

public class CreateRouteFunctionalitySagas extends WorkflowFunctionality {
    private RouteDto createdRouteDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateRouteFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateRouteRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateRouteRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createRouteStep = new SagaStep("createRouteStep", () -> {
            CreateRouteCommand cmd = new CreateRouteCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), createRequest);
            RouteDto createdRouteDto = (RouteDto) commandGateway.send(cmd);
            setCreatedRouteDto(createdRouteDto);
        });

        workflow.addStep(createRouteStep);
    }
    public RouteDto getCreatedRouteDto() {
        return createdRouteDto;
    }

    public void setCreatedRouteDto(RouteDto createdRouteDto) {
        this.createdRouteDto = createdRouteDto;
    }
}
