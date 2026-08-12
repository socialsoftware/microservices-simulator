package pt.ulisboa.tecnico.socialsoftware.answers.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

public class UpdateExecutionCommand extends Command {
    private final ExecutionDto executionDto;

    public UpdateExecutionCommand(UnitOfWork unitOfWork, String serviceName, ExecutionDto executionDto) {
        super(unitOfWork, serviceName, null);
        this.executionDto = executionDto;
    }

    public ExecutionDto getExecutionDto() { return executionDto; }
}
