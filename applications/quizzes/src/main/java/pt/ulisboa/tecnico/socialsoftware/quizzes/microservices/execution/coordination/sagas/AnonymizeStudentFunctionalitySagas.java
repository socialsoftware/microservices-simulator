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
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.AnonymizeStudentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class AnonymizeStudentFunctionalitySagas extends WorkflowFunctionality {

    private CourseExecutionDto courseExecution;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnonymizeStudentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
                                              CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId,
                              SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseExecutionStep = new SagaStep("getCourseExecutionStep", () -> {
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
<<<<<<< HEAD
            SagaCommand sagaCommand = new SagaCommand(getCourseExecutionByIdCommand);
            sagaCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            sagaCommand.setForbiddenStates(List.of(GenericSagaState.IN_SAGA));
            commandGateway.send(sagaCommand);
=======
            getCourseExecutionByIdCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            getCourseExecutionByIdCommand.setForbiddenStates(new ArrayList<>(List.of(CourseExecutionSagaState.READ_COURSE)));
            commandGateway.send(getCourseExecutionByIdCommand);
>>>>>>> 8e6c2b1c (fix(Execution): change forbidden-state declarations in remove and anonymize student)
        });

        getCourseExecutionStep.registerCompensation(() -> {
            Logger.getLogger(AnonymizeStudentFunctionalitySagas.class.getName()).info("Compensating getCourseExecutionStep");
            Command command = new Command(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep anonymizeStudentStep = new SagaStep("anonymizeStudentStep", () -> {
            AnonymizeStudentCommand anonymizeStudentCommand = new AnonymizeStudentCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, userAggregateId);
            commandGateway.send(anonymizeStudentCommand);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(anonymizeStudentStep);
    }

    public CourseExecutionDto getCourseExecution() {
        return this.courseExecution;
    }

    public void setCourseExecution(CourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}
