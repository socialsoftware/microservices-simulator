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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.webapi.requestDtos.CreateStationRequestDto;

public class CreateStationFunctionalitySagas extends WorkflowFunctionality {
    private StationDto createdStationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateStationRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateStationRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createStationStep = new SagaStep("createStationStep", () -> {
            CreateStationCommand cmd = new CreateStationCommand(unitOfWork, ServiceMapping.STATION.getServiceName(), createRequest);
            StationDto createdStationDto = (StationDto) commandGateway.send(cmd);
            setCreatedStationDto(createdStationDto);
        });

        workflow.addStep(createStationStep);
    }
    public StationDto getCreatedStationDto() {
        return createdStationDto;
    }

    public void setCreatedStationDto(StationDto createdStationDto) {
        this.createdStationDto = createdStationDto;
    }
}
