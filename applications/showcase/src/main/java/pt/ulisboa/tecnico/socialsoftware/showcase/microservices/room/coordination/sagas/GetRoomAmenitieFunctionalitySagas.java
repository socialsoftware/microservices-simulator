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

public class GetRoomAmenitieFunctionalitySagas extends WorkflowFunctionality {
    private RoomAmenityDto amenitieDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetRoomAmenitieFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer roomId, Integer code, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomId, code, unitOfWork);
    }

    public void buildWorkflow(Integer roomId, Integer code, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAmenitieStep = new SagaStep("getAmenitieStep", () -> {
            GetRoomAmenitieCommand cmd = new GetRoomAmenitieCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), roomId, code);
            RoomAmenityDto amenitieDto = (RoomAmenityDto) commandGateway.send(cmd);
            setAmenitieDto(amenitieDto);
        });

        workflow.addStep(getAmenitieStep);
    }
    public RoomAmenityDto getAmenitieDto() {
        return amenitieDto;
    }

    public void setAmenitieDto(RoomAmenityDto amenitieDto) {
        this.amenitieDto = amenitieDto;
    }
}
