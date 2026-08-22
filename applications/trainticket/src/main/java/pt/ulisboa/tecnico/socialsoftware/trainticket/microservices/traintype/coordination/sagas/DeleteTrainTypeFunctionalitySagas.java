package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.DeleteTrainTypeCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypeByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.states.TrainTypeSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteTrainTypeFunctionalitySagas extends WorkflowFunctionality {
    private TrainTypeDto trainTypeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteTrainTypeFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             Integer trainTypeAggregateId,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(trainTypeAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer trainTypeAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTrainTypeStep = new SagaStep("getTrainTypeStep", () -> {
            GetTrainTypeByIdCommand readCmd = new GetTrainTypeByIdCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(), trainTypeAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TrainTypeSagaState.IN_DELETE_TRAIN_TYPE);
            this.trainTypeDto = (TrainTypeDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteTrainTypeStep = new SagaStep("deleteTrainTypeStep", () -> {
            DeleteTrainTypeCommand cmd = new DeleteTrainTypeCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(), trainTypeAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTrainTypeStep)));

        this.workflow.addStep(getTrainTypeStep);
        this.workflow.addStep(deleteTrainTypeStep);
    }

    public TrainTypeDto getTrainTypeDto() {
        return trainTypeDto;
    }
}
