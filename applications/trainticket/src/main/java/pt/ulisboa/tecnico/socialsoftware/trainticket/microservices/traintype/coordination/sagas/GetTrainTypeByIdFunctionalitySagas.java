package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypeByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;

public class GetTrainTypeByIdFunctionalitySagas extends WorkflowFunctionality {
    private TrainTypeDto trainTypeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTrainTypeByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              Integer trainTypeAggregateId,
                                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(trainTypeAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer trainTypeAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTrainTypeStep = new SagaStep("getTrainTypeStep", () -> {
            GetTrainTypeByIdCommand cmd = new GetTrainTypeByIdCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(), trainTypeAggregateId);
            this.trainTypeDto = (TrainTypeDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getTrainTypeStep);
    }

    public TrainTypeDto getTrainTypeDto() {
        return trainTypeDto;
    }
}
