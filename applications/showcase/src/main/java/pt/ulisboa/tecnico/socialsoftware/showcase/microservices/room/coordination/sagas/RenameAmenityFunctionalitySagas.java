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

public class RenameAmenityFunctionalitySagas extends WorkflowFunctionality {
    
        private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final RoomService roomService;
    private final SagaUnitOfWork unitOfWork;
    private final CommandGateway commandGateway;

    public RenameAmenityFunctionalitySagas(SagaUnitOfWorkService sagaUnitOfWorkService, RoomService roomService, Integer roomId, Integer amenityCode, String newName, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.roomService = roomService;
        this.unitOfWork = unitOfWork;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomId, amenityCode, newName);
    }

    public void buildWorkflow(Integer roomId, Integer amenityCode, String newName) {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        SagaStep renameAmenityStep = new SagaStep("renameAmenityStep", () -> {
            this.roomService.renameAmenity(roomId, amenityCode, newName, this.unitOfWork);
        });
        this.workflow.addStep(renameAmenityStep);
    }

}
