package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddExecutionUserFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionUserDto addedUserDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddExecutionUserFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, Integer executionId, Integer userAggregateId, ExecutionUserDto userDto) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, Integer userAggregateId, ExecutionUserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addUserStep = new SagaSyncStep("addUserStep", () -> {
            ExecutionUserDto addedUserDto = executionService.addExecutionUser(executionId, userAggregateId, userDto, unitOfWork);
            setAddedUserDto(addedUserDto);
        });

        workflow.addStep(addUserStep);
    }
    public ExecutionUserDto getAddedUserDto() {
        return addedUserDto;
    }

    public void setAddedUserDto(ExecutionUserDto addedUserDto) {
        this.addedUserDto = addedUserDto;
    }
}
