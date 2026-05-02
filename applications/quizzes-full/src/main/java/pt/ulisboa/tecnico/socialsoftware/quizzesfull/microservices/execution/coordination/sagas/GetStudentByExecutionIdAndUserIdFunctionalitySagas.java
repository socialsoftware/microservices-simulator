package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudentDto;

public class GetStudentByExecutionIdAndUserIdFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionStudentDto executionStudentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetStudentByExecutionIdAndUserIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                              Integer executionAggregateId, Integer userId,
                                                              SagaUnitOfWork unitOfWork,
                                                              CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStudentStep = new SagaStep("getStudentStep", () -> {
            GetStudentByExecutionIdAndUserIdCommand cmd = new GetStudentByExecutionIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, userId);
            this.executionStudentDto = (ExecutionStudentDto) commandGateway.send(cmd);
        });

        workflow.addStep(getStudentStep);
    }

    public ExecutionStudentDto getExecutionStudentDto() {
        return executionStudentDto;
    }
}
