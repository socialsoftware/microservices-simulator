package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateExecutionUserFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionUserDto updatedUserDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateExecutionUserFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, Integer executionId, Integer userAggregateId, ExecutionUserDto userDto) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, Integer userAggregateId, ExecutionUserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateUserStep = new SagaSyncStep("updateUserStep", () -> {
            ExecutionUserDto updatedUserDto = executionService.updateExecutionUser(executionId, userAggregateId, userDto, unitOfWork);
            setUpdatedUserDto(updatedUserDto);
        });

        workflow.addStep(updateUserStep);
    }
    public ExecutionUserDto getUpdatedUserDto() {
        return updatedUserDto;
    }

    public void setUpdatedUserDto(ExecutionUserDto updatedUserDto) {
        this.updatedUserDto = updatedUserDto;
    }
}
