package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetAndOrCreateCourseRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.CreateCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

public class CreateCourseExecutionFunctionalityTCC extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private CourseExecutionDto createdCourseExecution;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateCourseExecutionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                                 CourseExecutionDto courseExecutionDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionDto, unitOfWork);
    }

    public void buildWorkflow(CourseExecutionDto courseExecutionDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetAndOrCreateCourseRemoteCommand getAndOrCreateCourseRemoteCommand = new GetAndOrCreateCourseRemoteCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(),  courseExecutionDto);
            this.courseExecutionDto = (CourseExecutionDto) commandGateway.send(getAndOrCreateCourseRemoteCommand);

            CreateCourseExecutionCommand createCourseExecutionCommand = new CreateCourseExecutionCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), this.courseExecutionDto);
            this.createdCourseExecution = (CourseExecutionDto) commandGateway.send(createCourseExecutionCommand);
        });

        workflow.addStep(step);
    }

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }

    public void setCourseExecutionDto(CourseExecutionDto courseExecutionDto) {
        this.courseExecutionDto = courseExecutionDto;
    }

    public CourseExecutionDto getCreatedCourseExecution() {
        return createdCourseExecution;
    }

    public void setCreatedCourseExecution(CourseExecutionDto createdCourseExecution) {
        this.createdCourseExecution = createdCourseExecution;
    }
}
