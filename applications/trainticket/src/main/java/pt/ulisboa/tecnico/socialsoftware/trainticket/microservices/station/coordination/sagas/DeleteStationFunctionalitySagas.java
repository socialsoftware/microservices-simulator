package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.DeleteStationCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.GetStationByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.states.StationSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteStationFunctionalitySagas extends WorkflowFunctionality {
    private StationDto stationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                           Integer stationAggregateId,
                                           SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(stationAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer stationAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStationStep = new SagaStep("getStationStep", () -> {
            GetStationByIdCommand readCmd = new GetStationByIdCommand(
                    unitOfWork, ServiceMapping.STATION.getServiceName(), stationAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(StationSagaState.IN_DELETE_STATION);
            this.stationDto = (StationDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteStationStep = new SagaStep("deleteStationStep", () -> {
            DeleteStationCommand cmd = new DeleteStationCommand(
                    unitOfWork, ServiceMapping.STATION.getServiceName(), stationAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getStationStep)));

        this.workflow.addStep(getStationStep);
        this.workflow.addStep(deleteStationStep);
    }

    public StationDto getStationDto() {
        return stationDto;
    }
}
