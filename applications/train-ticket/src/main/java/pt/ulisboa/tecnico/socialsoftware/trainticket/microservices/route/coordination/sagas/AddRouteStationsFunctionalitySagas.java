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
import java.util.List;

public class AddRouteStationsFunctionalitySagas extends WorkflowFunctionality {
    private List<RouteStationDto> addedStationDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddRouteStationsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer routeId, List<RouteStationDto> stationDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(routeId, stationDtos, unitOfWork);
    }

    public void buildWorkflow(Integer routeId, List<RouteStationDto> stationDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addStationsStep = new SagaStep("addStationsStep", () -> {
            AddRouteStationsCommand cmd = new AddRouteStationsCommand(unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeId, stationDtos);
            List<RouteStationDto> addedStationDtos = (List<RouteStationDto>) commandGateway.send(cmd);
            setAddedStationDtos(addedStationDtos);
        });

        workflow.addStep(addStationsStep);
    }
    public List<RouteStationDto> getAddedStationDtos() {
        return addedStationDtos;
    }

    public void setAddedStationDtos(List<RouteStationDto> addedStationDtos) {
        this.addedStationDtos = addedStationDtos;
    }
}
