package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;

public class CreateCourseCommand extends Command {
    private CourseDto courseDto;

    public CreateCourseCommand(UnitOfWork unitOfWork, String serviceName, CourseDto courseDto) {
        super(unitOfWork, serviceName, null);
        this.courseDto = courseDto;
    }

    public CourseDto getCourseDto() {
        return courseDto;
    }
}
