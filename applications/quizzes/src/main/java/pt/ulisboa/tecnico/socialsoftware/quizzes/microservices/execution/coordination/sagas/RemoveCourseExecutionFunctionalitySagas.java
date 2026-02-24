package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.RemoveCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class RemoveCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private CourseExecutionDto courseExecution;
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
            GetCourseExecutionByIdCommand getCourseExecutionCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId);
            getCourseExecutionCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            CourseExecutionDto courseExecution = (CourseExecutionDto) commandGateway.send(getCourseExecutionCommand);
            this.setCourseExecution(courseExecution);
        });

        getCourseExecutionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep removeCourseExecutionStep = new SagaStep("removeCourseExecutionStep", () -> {
            RemoveCourseExecutionCommand removeCourseExecutionCommand = new RemoveCourseExecutionCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId);
            commandGateway.send(removeCourseExecutionCommand);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(removeCourseExecutionStep);
    }

    public CourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(CourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}