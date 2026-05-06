package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.topic.DeleteTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.causal.CausalTopic;

public class DeleteTopicFunctionalityTCC extends WorkflowFunctionality {
    private CausalTopic topic;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteTopicFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                       Integer topicAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            DeleteTopicCommand DeleteTopicCommand = new DeleteTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            commandGateway.send(DeleteTopicCommand);
        });

        workflow.addStep(step);
    }

    public CausalTopic getTopic() {
        return topic;
    }

    public void setTopic(CausalTopic topic) {
        this.topic = topic;
    }
}