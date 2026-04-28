package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.sagas.states.AnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto;

public class CreateAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto createdAnswerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateAnswerRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateAnswerRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createAnswerStep = new SagaStep("createAnswerStep", () -> {
            CreateAnswerCommand cmd = new CreateAnswerCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), createRequest);
            AnswerDto createdAnswerDto = (AnswerDto) commandGateway.send(cmd);
            setCreatedAnswerDto(createdAnswerDto);
        });

        workflow.addStep(createAnswerStep);
    }
    public AnswerDto getCreatedAnswerDto() {
        return createdAnswerDto;
    }

    public void setCreatedAnswerDto(AnswerDto createdAnswerDto) {
        this.createdAnswerDto = createdAnswerDto;
    }
}
