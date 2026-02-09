package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddExecutionUsersFunctionalitySagas extends WorkflowFunctionality {
    private List<ExecutionUserDto> addedUserDtos;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddExecutionUsersFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, Integer executionId, List<ExecutionUserDto> userDtos) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionId, userDtos, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, List<ExecutionUserDto> userDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addUsersStep = new SagaSyncStep("addUsersStep", () -> {
            List<ExecutionUserDto> addedUserDtos = executionService.addExecutionUsers(executionId, userDtos, unitOfWork);
            setAddedUserDtos(addedUserDtos);
        });

        workflow.addStep(addUsersStep);
    }
    public List<ExecutionUserDto> getAddedUserDtos() {
        return addedUserDtos;
    }

    public void setAddedUserDtos(List<ExecutionUserDto> addedUserDtos) {
        this.addedUserDtos = addedUserDtos;
    }
}
