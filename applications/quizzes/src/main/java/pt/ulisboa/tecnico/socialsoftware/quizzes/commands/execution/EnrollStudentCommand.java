package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
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
