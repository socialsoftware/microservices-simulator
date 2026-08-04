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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.webapi.requestDtos.CreateTrainRequestDto;

public class CreateTrainFunctionalitySagas extends WorkflowFunctionality {
    private TrainDto createdTrainDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateTrainFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateTrainRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTrainRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createTrainStep = new SagaStep("createTrainStep", () -> {
            CreateTrainCommand cmd = new CreateTrainCommand(unitOfWork, ServiceMapping.TRAIN.getServiceName(), createRequest);
            TrainDto createdTrainDto = (TrainDto) commandGateway.send(cmd);
            setCreatedTrainDto(createdTrainDto);
        });

        workflow.addStep(createTrainStep);
    }
    public TrainDto getCreatedTrainDto() {
        return createdTrainDto;
    }

    public void setCreatedTrainDto(TrainDto createdTrainDto) {
        this.createdTrainDto = createdTrainDto;
    }
}
