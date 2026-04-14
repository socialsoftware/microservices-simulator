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
import java.util.List;

public class AddRoomAmenitiesFunctionalitySagas extends WorkflowFunctionality {
    private List<RoomAmenityDto> addedAmenitieDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddRoomAmenitiesFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer roomId, List<RoomAmenityDto> amenitieDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(roomId, amenitieDtos, unitOfWork);
    }

    public void buildWorkflow(Integer roomId, List<RoomAmenityDto> amenitieDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addAmenitiesStep = new SagaStep("addAmenitiesStep", () -> {
            AddRoomAmenitiesCommand cmd = new AddRoomAmenitiesCommand(unitOfWork, ServiceMapping.ROOM.getServiceName(), roomId, amenitieDtos);
            List<RoomAmenityDto> addedAmenitieDtos = (List<RoomAmenityDto>) commandGateway.send(cmd);
            setAddedAmenitieDtos(addedAmenitieDtos);
        });

        workflow.addStep(addAmenitiesStep);
    }
    public List<RoomAmenityDto> getAddedAmenitieDtos() {
        return addedAmenitieDtos;
    }

    public void setAddedAmenitieDtos(List<RoomAmenityDto> addedAmenitieDtos) {
        this.addedAmenitieDtos = addedAmenitieDtos;
    }
}
