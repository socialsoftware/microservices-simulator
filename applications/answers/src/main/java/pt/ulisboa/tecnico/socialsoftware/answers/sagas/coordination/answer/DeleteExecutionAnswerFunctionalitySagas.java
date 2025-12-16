package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.DeleteExecutionEvent;

public class DeleteExecutionAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerService answerService;
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    private Integer aggregateId;
    private Integer publisherAggregateId;
    private Integer executionAggregateId;
    private String acronym;
    private Integer publisherAggregateVersion;

    public DeleteExecutionAnswerFunctionalitySagas(AnswerService answerService, SagaUnitOfWorkService sagaUnitOfWorkService, Integer aggregateId, Integer publisherAggregateId, Integer executionAggregateId, String acronym, Integer publisherAggregateVersion, SagaUnitOfWork unitOfWork) {
        this.answerService = answerService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.aggregateId = aggregateId;
        this.publisherAggregateId = publisherAggregateId;
        this.executionAggregateId = executionAggregateId;
        this.acronym = acronym;
        this.publisherAggregateVersion = publisherAggregateVersion;
        this.buildWorkflow(aggregateId, answerId, unitOfWork);
    }

    public void buildWorkflow(Integer aggregateId, Integer answerId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);

        // TODO: Implement workflow steps for removeExecution
        // Example structure:
        // SagaSyncStep step1 = new SagaSyncStep("step1", () -> {
        //     // Step implementation
        // });
        // this.workflow.addStep(step1);
    }

}