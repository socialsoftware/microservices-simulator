package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetExecutionUserFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionUserDto userDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetExecutionUserFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, Integer executionId, Integer userAggregateId) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            ExecutionUserDto userDto = executionService.getExecutionUser(executionId, userAggregateId, unitOfWork);
            setUserDto(userDto);
        });

        workflow.addStep(getUserStep);
    }
    public ExecutionUserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(ExecutionUserDto userDto) {
        this.userDto = userDto;
    }
}
