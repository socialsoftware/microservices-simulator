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

public class AddStudentFunctionalitySagas extends WorkflowFunctionality {
    
    private Object userDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final SagaUnitOfWork unitOfWork;
    private final UserService userService;

    public AddStudentFunctionalitySagas(ExecutionService executionService, SagaUnitOfWorkService sagaUnitOfWorkService, SagaUnitOfWork unitOfWork, UserService userService) {
        this.executionService = executionService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.unitOfWork = unitOfWork;
        this.userService = userService;
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId) {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        SagaSyncStep getUserStepStep = new SagaSyncStep("getUserStep", () -> {
            userDto = this.userService.getUserById(null /* TODO: fix argument */, null /* TODO: fix argument */);
        });
        workflow.addStep(getUserStepStep);

        SagaSyncStep enrollStudentStepStep = new SagaSyncStep("enrollStudentStep", () -> {
            this.executionService.enrollStudent(null /* TODO: fix argument */, null /* TODO: fix argument */, null /* TODO: fix argument */);
        }, new ArrayList<>(Arrays.asList(getUserStepStep)));
        workflow.addStep(enrollStudentStepStep);


    }

}


