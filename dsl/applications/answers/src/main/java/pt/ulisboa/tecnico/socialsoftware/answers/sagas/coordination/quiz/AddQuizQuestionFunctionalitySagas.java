package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddQuizQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuizQuestionDto addedQuestionDto;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddQuizQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, Integer quizId, Integer questionAggregateId, QuizQuestionDto questionDto) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizId, questionAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, Integer questionAggregateId, QuizQuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addQuestionStep = new SagaSyncStep("addQuestionStep", () -> {
            QuizQuestionDto addedQuestionDto = quizService.addQuizQuestion(quizId, questionAggregateId, questionDto, unitOfWork);
            setAddedQuestionDto(addedQuestionDto);
        });

        workflow.addStep(addQuestionStep);
    }
    public QuizQuestionDto getAddedQuestionDto() {
        return addedQuestionDto;
    }

    public void setAddedQuestionDto(QuizQuestionDto addedQuestionDto) {
        this.addedQuestionDto = addedQuestionDto;
    }
}
