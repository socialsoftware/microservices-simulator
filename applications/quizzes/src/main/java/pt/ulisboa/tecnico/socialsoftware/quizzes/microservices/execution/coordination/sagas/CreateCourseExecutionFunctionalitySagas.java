package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetAndOrCreateCourseRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.CreateCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private CourseExecutionDto createdCourseExecution;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateCourseExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CourseExecutionDto courseExecutionDto, SagaUnitOfWork unitOfWork,
                                                   CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionDto, unitOfWork);
    }

    public void buildWorkflow(CourseExecutionDto courseExecutionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAndOrCreateCourseRemote = new SagaStep("getAndOrCreateCourseRemote", () -> {
            GetAndOrCreateCourseRemoteCommand getAndOrCreateCourseRemoteCommand = new GetAndOrCreateCourseRemoteCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(),  courseExecutionDto);
            this.courseExecutionDto = (CourseExecutionDto) commandGateway.send(getAndOrCreateCourseRemoteCommand);
        });

        SagaStep createCourseExecutionStep = new SagaStep("createCourseExecutionStep", () -> {
            CreateCourseExecutionCommand createCourseExecutionCommand = new CreateCourseExecutionCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), this.courseExecutionDto);
            CourseExecutionDto createdCourseExecution = (CourseExecutionDto) commandGateway.send(createCourseExecutionCommand);
            this.setCreatedCourseExecution(createdCourseExecution);
        }, new ArrayList<>(Arrays.asList(getAndOrCreateCourseRemote)));

        workflow.addStep(getAndOrCreateCourseRemote);
        workflow.addStep(createCourseExecutionStep);
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
