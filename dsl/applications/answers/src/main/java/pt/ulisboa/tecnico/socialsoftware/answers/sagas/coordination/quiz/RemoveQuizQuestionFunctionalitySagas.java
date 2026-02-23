package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveQuizQuestionFunctionalitySagas extends WorkflowFunctionality {
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveQuizQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, Integer quizId, Integer questionAggregateId) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizId, questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep removeQuestionStep = new SagaSyncStep("removeQuestionStep", () -> {
            quizService.removeQuizQuestion(quizId, questionAggregateId, unitOfWork);
        });

        workflow.addStep(removeQuestionStep);
    }
}
