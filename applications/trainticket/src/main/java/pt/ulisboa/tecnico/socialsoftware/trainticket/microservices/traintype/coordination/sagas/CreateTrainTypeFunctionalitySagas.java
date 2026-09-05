package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.CreateTrainTypeCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;

public class CreateTrainTypeFunctionalitySagas extends WorkflowFunctionality {
    private TrainTypeDto createdTrainTypeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateTrainTypeFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             TrainTypeDto trainTypeDto,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(trainTypeDto, unitOfWork);
    }

    public void buildWorkflow(TrainTypeDto trainTypeDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createTrainTypeStep = new SagaStep("createTrainTypeStep", () -> {
            CreateTrainTypeCommand cmd = new CreateTrainTypeCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(), trainTypeDto);
            this.createdTrainTypeDto = (TrainTypeDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(createTrainTypeStep);
    }

    public TrainTypeDto getCreatedTrainTypeDto() {
        return createdTrainTypeDto;
    }
}
