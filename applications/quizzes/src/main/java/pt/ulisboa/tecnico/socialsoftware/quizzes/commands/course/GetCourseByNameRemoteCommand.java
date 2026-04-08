package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

public class GetCourseByNameRemoteCommand extends Command {
    private CourseExecutionDto courseExecutionDto;

    public GetCourseByNameRemoteCommand(UnitOfWork unitOfWork, String serviceName,
            CourseExecutionDto courseExecutionDto) {
        super(unitOfWork, serviceName, null);
        this.courseExecutionDto = courseExecutionDto;
    }

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }
}
