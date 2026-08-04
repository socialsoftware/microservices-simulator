package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.station.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllStationsFunctionalitySagas extends WorkflowFunctionality {
    private List<StationDto> stations;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllStationsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllStationsStep = new SagaStep("getAllStationsStep", () -> {
            GetAllStationsCommand cmd = new GetAllStationsCommand(unitOfWork, ServiceMapping.STATION.getServiceName());
            List<StationDto> stations = (List<StationDto>) commandGateway.send(cmd);
            setStations(stations);
        });

        workflow.addStep(getAllStationsStep);
    }
    public List<StationDto> getStations() {
        return stations;
    }

    public void setStations(List<StationDto> stations) {
        this.stations = stations;
    }
}
