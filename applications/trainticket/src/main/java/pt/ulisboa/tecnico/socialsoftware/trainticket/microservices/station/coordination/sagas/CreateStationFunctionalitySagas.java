package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.CreateStationCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;

public class CreateStationFunctionalitySagas extends WorkflowFunctionality {
    private StationDto createdStationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                           StationDto stationDto,
                                           SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(stationDto, unitOfWork);
    }

    public void buildWorkflow(StationDto stationDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createStationStep = new SagaStep("createStationStep", () -> {
            CreateStationCommand cmd = new CreateStationCommand(
                    unitOfWork, ServiceMapping.STATION.getServiceName(), stationDto);
            this.createdStationDto = (StationDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(createStationStep);
    }

    public StationDto getCreatedStationDto() {
        return createdStationDto;
    }
}
