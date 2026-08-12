package pt.ulisboa.tecnico.socialsoftware.answers.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;

public class UpdateExecutionUserCommand extends Command {
    private final Integer executionId;
    private final Integer userAggregateId;
    private final ExecutionUserDto userDto;

    public UpdateExecutionUserCommand(UnitOfWork unitOfWork, String serviceName, Integer executionId, Integer userAggregateId, ExecutionUserDto userDto) {
        super(unitOfWork, serviceName, null);
        this.executionId = executionId;
        this.userAggregateId = userAggregateId;
        this.userDto = userDto;
    }

    public Integer getExecutionId() { return executionId; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public ExecutionUserDto getUserDto() { return userDto; }
}
