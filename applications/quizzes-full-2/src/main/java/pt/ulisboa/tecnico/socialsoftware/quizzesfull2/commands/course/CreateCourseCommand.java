package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;

public class CreateCourseCommand extends Command {
    private CourseDto courseDto;

    protected CreateCourseCommand() {}

    public CreateCourseCommand(UnitOfWork unitOfWork, String serviceName, CourseDto courseDto) {
        super(unitOfWork, serviceName, null);
        this.courseDto = courseDto;
    }

    public CourseDto getCourseDto() {
        return courseDto;
    }

    public void setCourseDto(CourseDto courseDto) {
        this.courseDto = courseDto;
    }
}
