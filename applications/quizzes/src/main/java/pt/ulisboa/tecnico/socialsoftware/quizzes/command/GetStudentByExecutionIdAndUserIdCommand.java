package pt.ulisboa.tecnico.socialsoftware.quizzes.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class GetStudentByExecutionIdAndUserIdCommand implements Command {
    private final UnitOfWork unitOfWork;
    private final Integer executionAggregateId;
    private final Integer userAggregateId;
    private Command command; // for saga state methods
    private UserDto userDto;

    public GetStudentByExecutionIdAndUserIdCommand(UnitOfWork unitOfWork, Integer executionAggregateId, Integer userAggregateId) {
        this.unitOfWork = unitOfWork;
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public GetStudentByExecutionIdAndUserIdCommand(UnitOfWork unitOfWork, Integer executionAggregateId, Integer userAggregateId, Command command) {
        this(unitOfWork, executionAggregateId, userAggregateId);
        this.command = command;
    }

    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public Command getCommand() {
        return command;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    //    @Override
//    public void execute() {
//        this.userDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionAggregateId, userAggregateId, unitOfWork);
//    }
//
//    public UserDto getUserDto() {
//        return userDto;
//    }
}
