package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.room.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto;

public class CreateRoomFunctionalitySagas extends WorkflowFunctionality {
    private RoomDto createdRoomDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateRoomFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateRoomRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateRoomRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createRoomStep = new SagaStep("createRoomStep", () -> {
            CreateRoomCommand cmd = new CreateRoomCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), createRequest);
            RoomDto createdRoomDto = (RoomDto) commandGateway.send(cmd);
            setCreatedRoomDto(createdRoomDto);
        });

        workflow.addStep(createRoomStep);
    }
    public RoomDto getCreatedRoomDto() {
        return createdRoomDto;
    }

    public void setCreatedRoomDto(RoomDto createdRoomDto) {
        this.createdRoomDto = createdRoomDto;
    }
}
