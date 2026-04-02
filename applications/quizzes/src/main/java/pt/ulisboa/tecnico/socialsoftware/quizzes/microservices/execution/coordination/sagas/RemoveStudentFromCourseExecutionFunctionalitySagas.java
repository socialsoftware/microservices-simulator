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
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.RemoveStudentFromCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveStudentFromCourseExecutionFunctionalitySagas extends WorkflowFunctionality {

    private CourseExecutionDto oldCourseExecution;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveStudentFromCourseExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                              Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
                                                              CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionAggregateId, Integer userAggregateId,
                              SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOldCourseExecutionStep = new SagaStep("getOldCourseExecutionStep", () -> {
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), courseExecutionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCourseExecutionByIdCommand);
            sagaCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            sagaCommand.setForbiddenStates(new ArrayList<>(List.of(CourseExecutionSagaState.READ_COURSE)));
            CourseExecutionDto oldCourseExecution = (CourseExecutionDto) commandGateway.send(sagaCommand);
            this.setOldCourseExecution(oldCourseExecution);
        });

        getOldCourseExecutionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), courseExecutionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep removeStudentStep = new SagaStep("removeStudentStep", () -> {
            RemoveStudentFromCourseExecutionCommand removeStudentFromCourseExecutionCommand = new RemoveStudentFromCourseExecutionCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), courseExecutionAggregateId, userAggregateId);
            commandGateway.send(removeStudentFromCourseExecutionCommand);
        }, new ArrayList<>(Arrays.asList(getOldCourseExecutionStep)));

        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(removeStudentStep);
    }

    public CourseExecutionDto getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecutionDto oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}
