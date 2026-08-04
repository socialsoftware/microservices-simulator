package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.train.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetTrainByIdFunctionalitySagas extends WorkflowFunctionality {
    private TrainDto trainDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetTrainByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer trainAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(trainAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer trainAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTrainStep = new SagaStep("getTrainStep", () -> {
            GetTrainByIdCommand cmd = new GetTrainByIdCommand(unitOfWork, ServiceMapping.TRAIN.getServiceName(), trainAggregateId);
            TrainDto trainDto = (TrainDto) commandGateway.send(cmd);
            setTrainDto(trainDto);
        });

        workflow.addStep(getTrainStep);
    }
    public TrainDto getTrainDto() {
        return trainDto;
    }

    public void setTrainDto(TrainDto trainDto) {
        this.trainDto = trainDto;
    }
}
