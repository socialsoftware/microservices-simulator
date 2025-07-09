package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class GetStudentByExecutionIdAndUserIdCommand extends Command {
    private CourseExecutionService courseExecutionService;
    private Integer executionAggregateId;
    private Integer userAggregateId;
    private UserDto userDto;

    public GetStudentByExecutionIdAndUserIdCommand(UnitOfWork unitOfWork, CourseExecutionService courseExecutionService, Integer executionAggregateId, Integer userAggregateId) {
        super(unitOfWork);
        this.courseExecutionService = courseExecutionService;
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
    }

    @Override
    public void execute() {
        this.userDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionAggregateId, userAggregateId, unitOfWork);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
