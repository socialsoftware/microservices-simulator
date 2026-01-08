package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteQuizFunctionalitySagas extends WorkflowFunctionality {
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteQuizFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, Integer quizAggregateId) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteQuizStep = new SagaSyncStep("deleteQuizStep", () -> {
            quizService.deleteQuiz(quizAggregateId, unitOfWork);
        });

        workflow.addStep(deleteQuizStep);

    }

}
