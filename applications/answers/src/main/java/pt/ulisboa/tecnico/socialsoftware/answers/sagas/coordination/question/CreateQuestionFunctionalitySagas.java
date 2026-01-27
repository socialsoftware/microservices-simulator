package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuestionRequestDto;

public class CreateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto createdQuestionDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, CreateQuestionRequestDto createRequest) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateQuestionRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createQuestionStep = new SagaSyncStep("createQuestionStep", () -> {
            QuestionDto createdQuestionDto = questionService.createQuestion(createRequest, unitOfWork);
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
