package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddExecutionUsersFunctionalitySagas extends WorkflowFunctionality {
    private List<ExecutionUserDto> addedUserDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddExecutionUsersFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer executionId, List<ExecutionUserDto> userDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionId, userDtos, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, List<ExecutionUserDto> userDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addUsersStep = new SagaStep("addUsersStep", () -> {
            AddExecutionUsersCommand cmd = new AddExecutionUsersCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId, userDtos);
            List<ExecutionUserDto> addedUserDtos = (List<ExecutionUserDto>) commandGateway.send(cmd);
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
