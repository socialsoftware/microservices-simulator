package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetQuizQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuizQuestionDto questionDto;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetQuizQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, Integer quizId, Integer questionAggregateId) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizId, questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            QuizQuestionDto questionDto = quizService.getQuizQuestion(quizId, questionAggregateId, unitOfWork);
            setQuestionDto(questionDto);
        });

        workflow.addStep(getQuestionStep);
    }
    public QuizQuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuizQuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}
