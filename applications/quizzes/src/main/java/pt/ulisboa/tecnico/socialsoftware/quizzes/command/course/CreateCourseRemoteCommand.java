package pt.ulisboa.tecnico.socialsoftware.quizzes.command.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

public class CreateCourseRemoteCommand extends Command {
    private CourseExecutionDto courseExecutionDto;

    public CreateCourseRemoteCommand(UnitOfWork unitOfWork, String serviceName, CourseExecutionDto courseExecutionDto) {
        super(unitOfWork, serviceName, null);
        this.courseExecutionDto = courseExecutionDto;
    }

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }
}
