package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.route.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddRouteStationFunctionalitySagas extends WorkflowFunctionality {
    private RouteStationDto addedStationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddRouteStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer routeId, Integer stationAggregateId, RouteStationDto stationDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(routeId, stationAggregateId, stationDto, unitOfWork);
    }

    public void buildWorkflow(Integer routeId, Integer stationAggregateId, RouteStationDto stationDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addStationStep = new SagaStep("addStationStep", () -> {
            AddRouteStationCommand cmd = new AddRouteStationCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeId, stationAggregateId, stationDto);
            RouteStationDto addedStationDto = (RouteStationDto) commandGateway.send(cmd);
            setAddedStationDto(addedStationDto);
        });

        workflow.addStep(addStationStep);
    }
    public RouteStationDto getAddedStationDto() {
        return addedStationDto;
    }

    public void setAddedStationDto(RouteStationDto addedStationDto) {
        this.addedStationDto = addedStationDto;
    }
}
