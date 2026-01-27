package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto updatedQuizDto;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateQuizFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, QuizDto quizDto) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizDto, unitOfWork);
    }

    public void buildWorkflow(QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateQuizStep = new SagaSyncStep("updateQuizStep", () -> {
            QuizDto updatedQuizDto = quizService.updateQuiz(quizDto);
            setUpdatedQuizDto(updatedQuizDto);
        });

        workflow.addStep(updateQuizStep);

    }

    public QuizDto getUpdatedQuizDto() {
        return updatedQuizDto;
    }

    public void setUpdatedQuizDto(QuizDto updatedQuizDto) {
        this.updatedQuizDto = updatedQuizDto;
    }
}
