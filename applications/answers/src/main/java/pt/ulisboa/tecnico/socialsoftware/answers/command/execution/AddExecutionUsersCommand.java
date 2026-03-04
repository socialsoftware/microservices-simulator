package pt.ulisboa.tecnico.socialsoftware.answers.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import java.util.List;

public class AddExecutionUsersCommand extends Command {
    private final Integer executionId;
    private final List<ExecutionUserDto> userDtos;

    public AddExecutionUsersCommand(UnitOfWork unitOfWork, String serviceName, Integer executionId, List<ExecutionUserDto> userDtos) {
        super(unitOfWork, serviceName, null);
        this.executionId = executionId;
        this.userDtos = userDtos;
    }

    public Integer getExecutionId() { return executionId; }
    public List<ExecutionUserDto> getUserDtos() { return userDtos; }
}
