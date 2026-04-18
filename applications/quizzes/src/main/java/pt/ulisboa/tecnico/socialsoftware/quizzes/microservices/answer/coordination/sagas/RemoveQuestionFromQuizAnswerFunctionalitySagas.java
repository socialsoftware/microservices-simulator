package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer.RemoveQuestionFromQuizAnswerCommand;

public class RemoveQuestionFromQuizAnswerFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveQuestionFromQuizAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer quizAnswerAggregateId, Integer questionAggregateId, Long publisherAggregateVersion,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(quizAnswerAggregateId, questionAggregateId, publisherAggregateVersion, unitOfWork);
    }

    private void buildWorkflow(Integer quizAnswerAggregateId, Integer questionAggregateId,
            Long publisherAggregateVersion,
            SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep step = new SagaStep("removeQuestionFromQuizAnswer", () -> {
            RemoveQuestionFromQuizAnswerCommand command = new RemoveQuestionFromQuizAnswerCommand(unitOfWork,
                    ServiceMapping.ANSWER.getServiceName(), quizAnswerAggregateId, questionAggregateId,
                    publisherAggregateVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
