package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.room.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.states.RoomSagaState;

public class UpdateRoomFunctionalitySagas extends WorkflowFunctionality {
    private RoomDto updatedRoomDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateRoomFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, RoomDto roomDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomDto, unitOfWork);
    }

    public void buildWorkflow(RoomDto roomDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateRoomStep = new SagaStep("updateRoomStep", () -> {
            unitOfWorkService.verifySagaState(roomDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(RoomSagaState.READ_ROOM, RoomSagaState.UPDATE_ROOM, RoomSagaState.DELETE_ROOM)));
            unitOfWorkService.registerSagaState(roomDto.getAggregateId(), RoomSagaState.UPDATE_ROOM, unitOfWork);
            UpdateRoomCommand cmd = new UpdateRoomCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), roomDto);
            RoomDto updatedRoomDto = (RoomDto) commandGateway.send(cmd);
            setUpdatedRoomDto(updatedRoomDto);
        });

        workflow.addStep(updateRoomStep);
    }
    public RoomDto getUpdatedRoomDto() {
        return updatedRoomDto;
    }

    public void setUpdatedRoomDto(RoomDto updatedRoomDto) {
        this.updatedRoomDto = updatedRoomDto;
    }
}
