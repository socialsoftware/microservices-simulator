package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.UpdateExecutionStudentNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.USER_MISSING_NAME;

public class UpdateStudentNameFunctionalitySagas extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateStudentNameFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer executionAggregateId, Integer userAggregateId, UserDto userDto, SagaUnitOfWork unitOfWork,
                                               CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, UserDto userDto,
            SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        if (userDto.getName() == null) {
            throw new QuizzesException(USER_MISSING_NAME);
        }

        SagaStep updateStudentNameStep = new SagaStep("updateStudentNameStep", () -> {
            UpdateExecutionStudentNameCommand updateExecutionStudentNameCommand = new UpdateExecutionStudentNameCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId, userAggregateId, userDto.getName());
            commandGateway.send(updateExecutionStudentNameCommand);
        });

        workflow.addStep(updateStudentNameStep);
    }
}
