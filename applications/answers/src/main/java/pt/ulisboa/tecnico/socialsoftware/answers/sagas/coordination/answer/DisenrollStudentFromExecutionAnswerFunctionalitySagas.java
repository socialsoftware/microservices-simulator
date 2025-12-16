package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.DisenrollStudentFromExecutionEvent;

public class DisenrollStudentFromExecutionAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerService answerService;
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    private Integer aggregateId;
    private Integer publisherAggregateId;
    private Integer executionAggregateId;
    private Integer studentAggregateId;
    private String studentName;
    private Integer publisherAggregateVersion;

    public DisenrollStudentFromExecutionAnswerFunctionalitySagas(AnswerService answerService, SagaUnitOfWorkService sagaUnitOfWorkService, Integer aggregateId, Integer publisherAggregateId, Integer executionAggregateId, Integer studentAggregateId, String studentName, Integer publisherAggregateVersion, SagaUnitOfWork unitOfWork) {
        this.answerService = answerService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.aggregateId = aggregateId;
        this.publisherAggregateId = publisherAggregateId;
        this.executionAggregateId = executionAggregateId;
        this.studentAggregateId = studentAggregateId;
        this.studentName = studentName;
        this.publisherAggregateVersion = publisherAggregateVersion;
        this.buildWorkflow(aggregateId, aggregateId, answerId, unitOfWork);
    }

    public void buildWorkflow(Integer aggregateId, Integer aggregateId, Integer answerId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);

        // TODO: Implement workflow steps for unenrollStudentFromExecution
        // Example structure:
        // SagaSyncStep step1 = new SagaSyncStep("step1", () -> {
        //     // Step implementation
        // });
        // this.workflow.addStep(step1);
    }

}