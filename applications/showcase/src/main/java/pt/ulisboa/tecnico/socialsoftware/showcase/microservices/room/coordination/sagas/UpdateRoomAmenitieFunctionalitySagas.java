package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.room.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateRoomAmenitieFunctionalitySagas extends WorkflowFunctionality {
    private RoomAmenityDto updatedAmenitieDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateRoomAmenitieFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer roomId, Integer code, RoomAmenityDto amenitieDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomId, code, amenitieDto, unitOfWork);
    }

    public void buildWorkflow(Integer roomId, Integer code, RoomAmenityDto amenitieDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateAmenitieStep = new SagaStep("updateAmenitieStep", () -> {
            UpdateRoomAmenitieCommand cmd = new UpdateRoomAmenitieCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), roomId, code, amenitieDto);
            RoomAmenityDto updatedAmenitieDto = (RoomAmenityDto) commandGateway.send(cmd);
            setUpdatedAmenitieDto(updatedAmenitieDto);
        });

        workflow.addStep(updateAmenitieStep);
    }
    public RoomAmenityDto getUpdatedAmenitieDto() {
        return updatedAmenitieDto;
    }

    public void setUpdatedAmenitieDto(RoomAmenityDto updatedAmenitieDto) {
        this.updatedAmenitieDto = updatedAmenitieDto;
    }
}
