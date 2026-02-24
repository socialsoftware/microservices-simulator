package pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class EnrollStudentCommand extends Command {
    private Integer courseExecutionAggregateId;
    private UserDto userDto;

    public EnrollStudentCommand(UnitOfWork unitOfWork, String serviceName, Integer courseExecutionAggregateId, UserDto userDto) {
        super(unitOfWork, serviceName, courseExecutionAggregateId);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.userDto = userDto;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
