package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.train.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class UpdateTrainFunctionalitySagas extends WorkflowFunctionality {
    private TrainDto updatedTrainDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateTrainFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TrainDto trainDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(trainDto, unitOfWork);
    }

    public void buildWorkflow(TrainDto trainDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTrainStep = new SagaStep("updateTrainStep", () -> {
            UpdateTrainCommand cmd = new UpdateTrainCommand(unitOfWork, ServiceMapping.TRAIN.getServiceName(), trainDto);
            TrainDto updatedTrainDto = (TrainDto) commandGateway.send(cmd);
            setUpdatedTrainDto(updatedTrainDto);
        });

        workflow.addStep(updateTrainStep);
    }
    public TrainDto getUpdatedTrainDto() {
        return updatedTrainDto;
    }

    public void setUpdatedTrainDto(TrainDto updatedTrainDto) {
        this.updatedTrainDto = updatedTrainDto;
    }
}
