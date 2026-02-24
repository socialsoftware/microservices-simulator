package pt.ulisboa.tecnico.socialsoftware.helloworld.command.task;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;

public class UpdateTaskCommand extends Command {
    private final TaskDto taskDto;

    public UpdateTaskCommand(UnitOfWork unitOfWork, String serviceName, TaskDto taskDto) {
        super(unitOfWork, serviceName, null);
        this.taskDto = taskDto;
    }

    public TaskDto getTaskDto() { return taskDto; }
}
