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

public class AddRoomAmenitieFunctionalitySagas extends WorkflowFunctionality {
    private RoomAmenityDto addedAmenitieDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddRoomAmenitieFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer roomId, Integer code, RoomAmenityDto amenitieDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomId, code, amenitieDto, unitOfWork);
    }

    public void buildWorkflow(Integer roomId, Integer code, RoomAmenityDto amenitieDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addAmenitieStep = new SagaStep("addAmenitieStep", () -> {
            AddRoomAmenitieCommand cmd = new AddRoomAmenitieCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), roomId, code, amenitieDto);
            RoomAmenityDto addedAmenitieDto = (RoomAmenityDto) commandGateway.send(cmd);
            setAddedAmenitieDto(addedAmenitieDto);
        });

        workflow.addStep(addAmenitieStep);
    }
    public RoomAmenityDto getAddedAmenitieDto() {
        return addedAmenitieDto;
    }

    public void setAddedAmenitieDto(RoomAmenityDto addedAmenitieDto) {
        this.addedAmenitieDto = addedAmenitieDto;
    }
}
