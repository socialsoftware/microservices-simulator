package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypesCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;

import java.util.List;

public class GetTrainTypesFunctionalitySagas extends WorkflowFunctionality {
    private List<TrainTypeDto> trainTypes;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTrainTypesFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                           SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(unitOfWork);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTrainTypesStep = new SagaStep("getTrainTypesStep", () -> {
            GetTrainTypesCommand cmd = new GetTrainTypesCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName());
            this.trainTypes = (List<TrainTypeDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getTrainTypesStep);
    }

    public List<TrainTypeDto> getTrainTypes() {
        return trainTypes;
    }
}
