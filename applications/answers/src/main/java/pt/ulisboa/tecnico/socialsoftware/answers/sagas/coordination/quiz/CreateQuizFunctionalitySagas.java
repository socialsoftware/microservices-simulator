package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuizRequestDto;

public class CreateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto createdQuizDto;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateQuizFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, CreateQuizRequestDto createRequest) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateQuizRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createQuizStep = new SagaSyncStep("createQuizStep", () -> {
            QuizDto createdQuizDto = quizService.createQuiz(createRequest, unitOfWork);
            setCreatedQuizDto(createdQuizDto);
        });

        workflow.addStep(createQuizStep);

    }

    public QuizDto getCreatedQuizDto() {
        return createdQuizDto;
    }

    public void setCreatedQuizDto(QuizDto createdQuizDto) {
        this.createdQuizDto = createdQuizDto;
    }
}
