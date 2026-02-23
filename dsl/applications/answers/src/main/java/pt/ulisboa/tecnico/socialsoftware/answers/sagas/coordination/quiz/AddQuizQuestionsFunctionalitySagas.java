package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddQuizQuestionsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuizQuestionDto> addedQuestionDtos;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddQuizQuestionsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, Integer quizId, List<QuizQuestionDto> questionDtos) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizId, questionDtos, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, List<QuizQuestionDto> questionDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addQuestionsStep = new SagaSyncStep("addQuestionsStep", () -> {
            List<QuizQuestionDto> addedQuestionDtos = quizService.addQuizQuestions(quizId, questionDtos, unitOfWork);
            setAddedQuestionDtos(addedQuestionDtos);
        });

        workflow.addStep(addQuestionsStep);
    }
    public List<QuizQuestionDto> getAddedQuestionDtos() {
        return addedQuestionDtos;
    }

    public void setAddedQuestionDtos(List<QuizQuestionDto> addedQuestionDtos) {
        this.addedQuestionDtos = addedQuestionDtos;
    }
}
