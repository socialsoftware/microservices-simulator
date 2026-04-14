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

public class GetRoomByIdFunctionalitySagas extends WorkflowFunctionality {
    private RoomDto roomDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetRoomByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer roomAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer roomAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getRoomStep = new SagaStep("getRoomStep", () -> {
            GetRoomByIdCommand cmd = new GetRoomByIdCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), roomAggregateId);
            RoomDto roomDto = (RoomDto) commandGateway.send(cmd);
            setRoomDto(roomDto);
        });

        workflow.addStep(getRoomStep);
    }
    public RoomDto getRoomDto() {
        return roomDto;
    }

    public void setRoomDto(RoomDto roomDto) {
        this.roomDto = roomDto;
    }
}
