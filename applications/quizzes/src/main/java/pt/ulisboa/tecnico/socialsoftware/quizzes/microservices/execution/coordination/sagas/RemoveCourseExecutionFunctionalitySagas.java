package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.UpdateCourseExecutionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.RemoveCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class RemoveCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private CourseExecutionDto courseExecution;
    private CourseDto course;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveCourseExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                   Integer executionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseExecutionStep = new SagaStep("getCourseExecutionStep", () -> {
            GetCourseExecutionByIdCommand getCourseExecutionCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCourseExecutionCommand);
            sagaCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            CourseExecutionDto courseExecution = (CourseExecutionDto) commandGateway.send(sagaCommand);
            this.setCourseExecution(courseExecution);
        });

        getCourseExecutionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), this.courseExecution.getCourseAggregateId());
            getCourseByIdCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            CourseDto courseDto = (CourseDto) commandGateway.send(getCourseByIdCommand);
            this.course = courseDto;

            if (courseDto.getCourseQuestionCount() > 0 && courseDto.getCourseExecutionCount() == 1) {
                throw new QuizzesException(QuizzesErrorMessage.CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT, executionAggregateId);
            }
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));

        getCourseStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), this.course.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep removeCourseExecutionStep = new SagaStep("removeCourseExecutionStep", () -> {
            RemoveCourseExecutionCommand removeCourseExecutionCommand = new RemoveCourseExecutionCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            commandGateway.send(removeCourseExecutionCommand);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep, getCourseStep)));

        // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
        SagaStep updateCourseExecutionCountStep = new SagaStep("updateCourseExecutionCountStep", () -> {
            UpdateCourseExecutionCountCommand cmd = new UpdateCourseExecutionCountCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), this.course.getAggregateId(), false);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getCourseStep, removeCourseExecutionStep)));

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(getCourseStep);
        workflow.addStep(removeCourseExecutionStep);
        workflow.addStep(updateCourseExecutionCountStep);
    }

    public CourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(CourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }

    public CourseDto getCourse() {
        return course;
    }

    public void setCourse(CourseDto course) {
        this.course = course;
    }
}
