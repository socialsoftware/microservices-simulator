package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.GetStationsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;

import java.util.List;

public class GetStationsFunctionalitySagas extends WorkflowFunctionality {
    private List<StationDto> stations;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetStationsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(unitOfWork);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStationsStep = new SagaStep("getStationsStep", () -> {
            GetStationsCommand cmd = new GetStationsCommand(
                    unitOfWork, ServiceMapping.STATION.getServiceName());
            this.stations = (List<StationDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getStationsStep);
    }

    public List<StationDto> getStations() {
        return stations;
    }
}
