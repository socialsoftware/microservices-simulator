package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.room.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.states.RoomSagaState;

public class DeleteRoomFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteRoomFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer roomAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer roomAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteRoomStep = new SagaStep("deleteRoomStep", () -> {
            unitOfWorkService.verifySagaState(roomAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(RoomSagaState.READ_ROOM, RoomSagaState.UPDATE_ROOM, RoomSagaState.DELETE_ROOM)));
            unitOfWorkService.registerSagaState(roomAggregateId, RoomSagaState.DELETE_ROOM, unitOfWork);
            DeleteRoomCommand cmd = new DeleteRoomCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), roomAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteRoomStep);
    }
}
