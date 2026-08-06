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

public class GetRouteStationFunctionalitySagas extends WorkflowFunctionality {
    private RouteStationDto stationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetRouteStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer routeId, Integer stationAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(routeId, stationAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer routeId, Integer stationAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStationStep = new SagaStep("getStationStep", () -> {
            GetRouteStationCommand cmd = new GetRouteStationCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeId, stationAggregateId);
            RouteStationDto stationDto = (RouteStationDto) commandGateway.send(cmd);
            setStationDto(stationDto);
        });

        workflow.addStep(getStationStep);
    }
    public RouteStationDto getStationDto() {
        return stationDto;
    }

    public void setStationDto(RouteStationDto stationDto) {
        this.stationDto = stationDto;
    }
}
