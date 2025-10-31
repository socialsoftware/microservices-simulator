package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

public class RemoveExecutionFunctionalitySagas extends WorkflowFunctionality {
    

    private final ExecutionService executionService;
    private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final SagaUnitOfWork unitOfWork;

    public RemoveExecutionFunctionalitySagas(ExecutionService executionService, SagaUnitOfWorkService sagaUnitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.executionService = executionService;
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


