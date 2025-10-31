package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

public class UpdateQuestionTopicsFunctionalitySagas extends WorkflowFunctionality {
    

    private final QuestionService questionService;
    private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final SagaUnitOfWork unitOfWork;

    public UpdateQuestionTopicsFunctionalitySagas(QuestionService questionService, SagaUnitOfWorkService sagaUnitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.unitOfWork = unitOfWork;
    }

    public void buildWorkflow() {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        // TODO: add Saga steps to implement the functionality
        // Example:
        // SagaSyncStep step = new SagaSyncStep("stepName", () -> {
        //     // call services and set result
        // });
        // workflow.addStep(step);
    }

}


