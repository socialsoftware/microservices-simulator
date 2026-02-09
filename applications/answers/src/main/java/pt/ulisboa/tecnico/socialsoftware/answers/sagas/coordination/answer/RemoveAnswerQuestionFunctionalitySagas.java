package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveAnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveAnswerQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Integer answerId, Integer questionAggregateId) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerId, questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep removeQuestionStep = new SagaSyncStep("removeQuestionStep", () -> {
            answerService.removeAnswerQuestion(answerId, questionAggregateId, unitOfWork);
        });

        workflow.addStep(removeQuestionStep);
    }
}
