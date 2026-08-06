package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.station.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class UpdateStationFunctionalitySagas extends WorkflowFunctionality {
    private StationDto updatedStationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, StationDto stationDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(stationDto, unitOfWork);
    }

    public void buildWorkflow(StationDto stationDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateStationStep = new SagaStep("updateStationStep", () -> {
            UpdateStationCommand cmd = new UpdateStationCommand(unitOfWork, ServiceMapping.STATION.getServiceName(), stationDto);
            StationDto updatedStationDto = (StationDto) commandGateway.send(cmd);
            setUpdatedStationDto(updatedStationDto);
        });

        workflow.addStep(updateStationStep);
    }
    public StationDto getUpdatedStationDto() {
        return updatedStationDto;
    }

    public void setUpdatedStationDto(StationDto updatedStationDto) {
        this.updatedStationDto = updatedStationDto;
    }
}
