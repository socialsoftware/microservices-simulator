package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

public class EnrollStudentInExecutionCommand extends Command {
    private Integer executionAggregateId;
    private UserDto userDto;

    protected EnrollStudentInExecutionCommand() {}

    public EnrollStudentInExecutionCommand(UnitOfWork unitOfWork, String serviceName,
                                           Integer executionAggregateId, UserDto userDto) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userDto = userDto;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }
}
