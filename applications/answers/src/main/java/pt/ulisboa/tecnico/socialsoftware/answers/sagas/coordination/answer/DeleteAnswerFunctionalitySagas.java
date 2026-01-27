package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteAnswerFunctionalitySagas extends WorkflowFunctionality {
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteAnswerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Integer answerAggregateId) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteAnswerStep = new SagaSyncStep("deleteAnswerStep", () -> {
            answerService.deleteAnswer(answerAggregateId);
        });

        workflow.addStep(deleteAnswerStep);

    }

}
