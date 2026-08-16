package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

public class EnrollStudentInExecutionCommand extends Command {
    private final Integer executionAggregateId;
    private final UserDto userDto;

    public EnrollStudentInExecutionCommand(UnitOfWork unitOfWork, String serviceName,
                                           Integer executionAggregateId, UserDto userDto) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userDto = userDto;
    }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public UserDto getUserDto() { return userDto; }
}
