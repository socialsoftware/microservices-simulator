package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.route.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class UpdateRouteStationFunctionalitySagas extends WorkflowFunctionality {
    private RouteStationDto updatedStationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateRouteStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer routeId, Integer stationAggregateId, RouteStationDto stationDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(routeId, stationAggregateId, stationDto, unitOfWork);
    }

    public void buildWorkflow(Integer routeId, Integer stationAggregateId, RouteStationDto stationDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateStationStep = new SagaStep("updateStationStep", () -> {
            UpdateRouteStationCommand cmd = new UpdateRouteStationCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeId, stationAggregateId, stationDto);
            RouteStationDto updatedStationDto = (RouteStationDto) commandGateway.send(cmd);
            setUpdatedStationDto(updatedStationDto);
        });

        workflow.addStep(updateStationStep);
    }
    public RouteStationDto getUpdatedStationDto() {
        return updatedStationDto;
    }

    public void setUpdatedStationDto(RouteStationDto updatedStationDto) {
        this.updatedStationDto = updatedStationDto;
    }
}
