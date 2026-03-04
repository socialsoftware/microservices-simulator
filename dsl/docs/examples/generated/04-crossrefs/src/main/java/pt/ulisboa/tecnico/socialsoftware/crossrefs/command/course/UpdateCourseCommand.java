package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;

public class UpdateCourseCommand extends Command {
    private final CourseDto courseDto;

    public UpdateCourseCommand(UnitOfWork unitOfWork, String serviceName, CourseDto courseDto) {
        super(unitOfWork, serviceName, null);
        this.courseDto = courseDto;
    }

    public CourseDto getCourseDto() { return courseDto; }
}
