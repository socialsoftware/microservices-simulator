package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto;

public class CreateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto createdQuestionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateQuestionRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateQuestionRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createQuestionStep = new SagaStep("createQuestionStep", () -> {
            CreateQuestionCommand cmd = new CreateQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), createRequest);
            QuestionDto createdQuestionDto = (QuestionDto) commandGateway.send(cmd);
            setCreatedQuestionDto(createdQuestionDto);
        });

        workflow.addStep(createQuestionStep);
    }
    public QuestionDto getCreatedQuestionDto() {
        return createdQuestionDto;
    }

    public void setCreatedQuestionDto(QuestionDto createdQuestionDto) {
        this.createdQuestionDto = createdQuestionDto;
    }
}
