package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.room.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService;

public class ReleaseFunctionalitySagas extends WorkflowFunctionality {
    
        private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final RoomService roomService;
    private final SagaUnitOfWork unitOfWork;
    private final CommandGateway commandGateway;

    public ReleaseFunctionalitySagas(SagaUnitOfWorkService sagaUnitOfWorkService, RoomService roomService, Integer roomId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.roomService = roomService;
        this.unitOfWork = unitOfWork;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomId);
    }

    public void buildWorkflow(Integer roomId) {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        SagaStep releaseStep = new SagaStep("releaseStep", () -> {
            this.roomService.release(roomId, this.unitOfWork);
        });
        this.workflow.addStep(releaseStep);
    }

}
