package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.RemoveUserFromQuizAnswerCommand;

public class RemoveUserFromQuizAnswerFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveUserFromQuizAnswerFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer quizAnswerAggregateId, Integer userAggregateId, Long publisherAggregateVersion,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(quizAnswerAggregateId, userAggregateId, publisherAggregateVersion, unitOfWork);
    }

    private void buildWorkflow(Integer quizAnswerAggregateId, Integer userAggregateId,
            Long publisherAggregateVersion,
            CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            RemoveUserFromQuizAnswerCommand command = new RemoveUserFromQuizAnswerCommand(unitOfWork,
                    ServiceMapping.ANSWER.getServiceName(), quizAnswerAggregateId, userAggregateId,
                    publisherAggregateVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
