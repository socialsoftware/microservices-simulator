package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.RemoveCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.CourseExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class RemoveCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private SagaCourseExecutionDto courseExecution;
    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveCourseExecutionFunctionalitySagas(CourseExecutionService courseExecutionService,
            SagaUnitOfWorkService unitOfWorkService,
            Integer executionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
            // SagaCourseExecutionDto courseExecution = (SagaCourseExecutionDto)
            // courseExecutionService.getCourseExecutionById(executionAggregateId,
            // unitOfWork);
            GetCourseExecutionByIdCommand getCourseExecutionCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId);
            getCourseExecutionCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            SagaCourseExecutionDto courseExecution = (SagaCourseExecutionDto) commandGateway.send(getCourseExecutionCommand);
            // unitOfWorkService.registerSagaState(executionAggregateId,
            // CourseExecutionSagaState.READ_COURSE, unitOfWork);
            this.setCourseExecution(courseExecution);
        });

        getCourseExecutionStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(executionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep removeCourseExecutionStep = new SagaSyncStep("removeCourseExecutionStep", () -> {
//            courseExecutionService.removeCourseExecution(executionAggregateId, unitOfWork);
            RemoveCourseExecutionCommand removeCourseExecutionCommand = new RemoveCourseExecutionCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId);
            commandGateway.send(removeCourseExecutionCommand);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(removeCourseExecutionStep);
    }

    public SagaCourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(SagaCourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}