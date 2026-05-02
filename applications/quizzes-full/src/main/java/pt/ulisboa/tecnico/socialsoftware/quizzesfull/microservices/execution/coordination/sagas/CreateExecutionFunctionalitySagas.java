package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.IncrementExecutionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.CreateExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto courseDto;
    private ExecutionDto createdExecutionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             String acronym, String academicTerm, Integer courseAggregateId,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(acronym, academicTerm, courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(String acronym, String academicTerm, Integer courseAggregateId,
                              SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCourseByIdCommand);
            sagaCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            this.courseDto = (CourseDto) commandGateway.send(sagaCommand);
        });

        getCourseStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep createExecutionStep = new SagaStep("createExecutionStep", () -> {
            ExecutionCourse executionCourse = new ExecutionCourse(this.courseDto);
            CreateExecutionCommand createExecutionCommand = new CreateExecutionCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(),
                    acronym, academicTerm, executionCourse);
            this.createdExecutionDto = (ExecutionDto) commandGateway.send(createExecutionCommand);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        SagaStep incrementCourseExecutionCountStep = new SagaStep("incrementCourseExecutionCountStep", () -> {
            IncrementExecutionCountCommand incrementCommand = new IncrementExecutionCountCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            commandGateway.send(incrementCommand);
        }, new ArrayList<>(Arrays.asList(createExecutionStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(createExecutionStep);
        workflow.addStep(incrementCourseExecutionCountStep);
    }

    public ExecutionDto getCreatedExecutionDto() { return createdExecutionDto; }
}
