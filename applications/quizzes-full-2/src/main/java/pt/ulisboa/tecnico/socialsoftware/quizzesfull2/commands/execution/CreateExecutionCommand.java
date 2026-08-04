package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;

public class CreateExecutionCommand extends Command {
    private ExecutionDto executionDto;
    private CourseDto courseDto;

    protected CreateExecutionCommand() {}

    public CreateExecutionCommand(UnitOfWork unitOfWork, String serviceName, ExecutionDto executionDto,
                                  CourseDto courseDto) {
        super(unitOfWork, serviceName, null);
        this.executionDto = executionDto;
        this.courseDto = courseDto;
    }

    public ExecutionDto getExecutionDto() {
        return executionDto;
    }

    public void setExecutionDto(ExecutionDto executionDto) {
        this.executionDto = executionDto;
    }

    public CourseDto getCourseDto() {
        return courseDto;
    }

    public void setCourseDto(CourseDto courseDto) {
        this.courseDto = courseDto;
    }
}
