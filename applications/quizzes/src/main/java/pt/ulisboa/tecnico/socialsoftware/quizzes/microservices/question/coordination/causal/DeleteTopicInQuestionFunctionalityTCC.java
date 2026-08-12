package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.question.RemoveTopicCommand;

public class DeleteTopicInQuestionFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteTopicInQuestionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer questionAggregateId, Integer topicAggregateId, Long eventVersion,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(questionAggregateId, topicAggregateId, eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer questionAggregateId, Integer topicAggregateId, Long eventVersion,
            CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            RemoveTopicCommand command = new RemoveTopicCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    questionAggregateId, topicAggregateId, eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
