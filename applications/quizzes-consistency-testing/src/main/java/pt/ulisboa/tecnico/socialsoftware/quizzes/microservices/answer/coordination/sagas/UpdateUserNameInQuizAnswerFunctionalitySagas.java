package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer.UpdateUserNameCommand;

public class UpdateUserNameInQuizAnswerFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateUserNameInQuizAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer quizAnswerAggregateId, Integer publisherAggregateId, Long publisherAggregateVersion,
            Integer studentAggregateId, String updatedName,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(quizAnswerAggregateId, publisherAggregateId, publisherAggregateVersion, studentAggregateId,
                updatedName, unitOfWork);
    }

    private void buildWorkflow(Integer quizAnswerAggregateId, Integer publisherAggregateId,
            Long publisherAggregateVersion,
            Integer studentAggregateId, String updatedName, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep step = new SagaStep("updateUserNameInQuizAnswer", () -> {
            UpdateUserNameCommand command = new UpdateUserNameCommand(unitOfWork,
                    ServiceMapping.ANSWER.getServiceName(), quizAnswerAggregateId, publisherAggregateId,
                    publisherAggregateVersion, studentAggregateId, updatedName);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
