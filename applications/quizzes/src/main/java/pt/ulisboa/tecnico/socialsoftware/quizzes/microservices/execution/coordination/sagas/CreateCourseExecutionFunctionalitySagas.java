package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.course.CreateCourseRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.course.DeleteCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.course.GetCourseByNameRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.course.UpdateCourseExecutionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution.CreateCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution.RemoveCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class CreateCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private CourseExecutionDto createdCourseExecution;
    private boolean courseWasCreated = false;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateCourseExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            CourseExecutionDto courseExecutionDto, SagaUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionDto, unitOfWork);
    }

    public void buildWorkflow(CourseExecutionDto courseExecutionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // Step 1: Look up course by name (read-only, no compensation needed)
        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByNameRemoteCommand getCourseByNameRemoteCommand = new GetCourseByNameRemoteCommand(unitOfWork,
                    ServiceMapping.COURSE.getServiceName(), courseExecutionDto);
            this.courseExecutionDto = (CourseExecutionDto) commandGateway.send(getCourseByNameRemoteCommand);
        });

        // Step 2: Create course only if step 1 found nothing; has compensation to undo
        // creation
        SagaStep createCourseStep = new SagaStep("createCourseStep", () -> {
            if (this.courseExecutionDto.getCourseAggregateId() == null) {
                CreateCourseRemoteCommand createCourseRemoteCommand = new CreateCourseRemoteCommand(unitOfWork,
                        ServiceMapping.COURSE.getServiceName(), this.courseExecutionDto);
                this.courseExecutionDto = (CourseExecutionDto) commandGateway.send(createCourseRemoteCommand);
                this.courseWasCreated = true;
            }
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        createCourseStep.registerCompensation(() -> {
            if (this.courseWasCreated && this.courseExecutionDto.getCourseAggregateId() != null) {
                Logger.getLogger(CreateCourseExecutionFunctionalitySagas.class.getName())
                        .info("Compensating createCourseStep: deleting course "
                                + this.courseExecutionDto.getCourseAggregateId());
                DeleteCourseCommand deleteCourseCommand = new DeleteCourseCommand(unitOfWork,
                        ServiceMapping.COURSE.getServiceName(), this.courseExecutionDto.getCourseAggregateId());
                commandGateway.send(deleteCourseCommand);
            }
        }, unitOfWork);

        // Step 3: Create the CourseExecution using the course ID from step 1 or 2
        SagaStep createCourseExecutionStep = new SagaStep("createCourseExecutionStep", () -> {
            CreateCourseExecutionCommand createCourseExecutionCommand = new CreateCourseExecutionCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), this.courseExecutionDto);
            CourseExecutionDto createdCourseExecution = (CourseExecutionDto) commandGateway.send(createCourseExecutionCommand);
            this.setCreatedCourseExecution(createdCourseExecution);
        }, new ArrayList<>(Arrays.asList(getCourseStep, createCourseStep)));

        createCourseExecutionStep.registerCompensation(() -> {
            if (this.createdCourseExecution != null && this.createdCourseExecution.getAggregateId() != null) {
                Logger.getLogger(CreateCourseExecutionFunctionalitySagas.class.getName())
                        .info("Compensating createCourseExecutionStep: removing execution "
                                + this.createdCourseExecution.getAggregateId());
                RemoveCourseExecutionCommand removeCourseExecutionCommand = new RemoveCourseExecutionCommand(unitOfWork,
                        ServiceMapping.EXECUTION.getServiceName(), this.createdCourseExecution.getAggregateId());
                commandGateway.send(removeCourseExecutionCommand);
            }
        }, unitOfWork);

        // Step 4: Increment course execution count — CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
        SagaStep updateCourseExecutionCountStep = new SagaStep("updateCourseExecutionCountStep", () -> {
            UpdateCourseExecutionCountCommand cmd = new UpdateCourseExecutionCountCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), this.courseExecutionDto.getCourseAggregateId(), true);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(createCourseExecutionStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(createCourseStep);
        workflow.addStep(createCourseExecutionStep);
        workflow.addStep(updateCourseExecutionCountStep);
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
