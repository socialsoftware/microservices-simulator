package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypeByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.UpdateTrainTypeCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.states.TrainTypeSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateTrainTypeFunctionalitySagas extends WorkflowFunctionality {
    private TrainTypeDto trainTypeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTrainTypeFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             Integer trainTypeAggregateId, TrainTypeDto updatedTrainTypeDto,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(trainTypeAggregateId, updatedTrainTypeDto, unitOfWork);
    }

    public void buildWorkflow(Integer trainTypeAggregateId, TrainTypeDto updatedTrainTypeDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTrainTypeStep = new SagaStep("getTrainTypeStep", () -> {
            GetTrainTypeByIdCommand readCmd = new GetTrainTypeByIdCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(), trainTypeAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TrainTypeSagaState.IN_UPDATE_TRAIN_TYPE);
            this.trainTypeDto = (TrainTypeDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateTrainTypeStep = new SagaStep("updateTrainTypeStep", () -> {
            UpdateTrainTypeCommand cmd = new UpdateTrainTypeCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(),
                    trainTypeAggregateId, updatedTrainTypeDto);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTrainTypeStep)));

        this.workflow.addStep(getTrainTypeStep);
        this.workflow.addStep(updateTrainTypeStep);
    }

    public TrainTypeDto getTrainTypeDto() {
        return trainTypeDto;
    }
}
