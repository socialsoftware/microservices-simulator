package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import java.util.ArrayList;
import java.util.Arrays;

public class AnonymizeStudentFunctionalitySagas extends WorkflowFunctionality {
    
    private Object courseExecution;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final SagaUnitOfWork unitOfWork;

    public AnonymizeStudentFunctionalitySagas(ExecutionService executionService, SagaUnitOfWorkService sagaUnitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.executionService = executionService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.unitOfWork = unitOfWork;
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId) {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        SagaSyncStep getExecutionStepStep = new SagaSyncStep("getExecutionStep", () -> {
            courseExecution = this.executionService.getExecutionByAggregateId([object Object], [object Object]);
            unitOfWorkService.registerSagaState([object Object], READ_COURSE, unitOfWork);
        });
        getExecutionStepStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState([object Object], NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
        workflow.addStep(getExecutionStepStep);

        SagaSyncStep anonymizeStudentStepStep = new SagaSyncStep("anonymizeStudentStep", () -> {
            this.executionService.anonymizeStudent([object Object], [object Object], [object Object]);
        }, new ArrayList<>(Arrays.asList(getExecutionStepStep)));
        workflow.addStep(anonymizeStudentStepStep);


    }

}


