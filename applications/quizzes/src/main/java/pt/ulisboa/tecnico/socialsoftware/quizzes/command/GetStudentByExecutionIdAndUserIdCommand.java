package pt.ulisboa.tecnico.socialsoftware.quizzes.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class GetStudentByExecutionIdAndUserIdCommand extends Command {
    private final Integer executionAggregateId;
    private final Integer userAggregateId;
    private UserDto userDto;

    public GetStudentByExecutionIdAndUserIdCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId, Integer userAggregateId) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
