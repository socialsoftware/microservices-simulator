package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.DeleteTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.CausalTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

public class DeleteTopicFunctionalityTCC extends WorkflowFunctionality {
    private CausalTopic topic;
    @SuppressWarnings("unused")
    private final TopicService topicService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteTopicFunctionalityTCC(TopicService topicService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer topicAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // topicService.deleteTopic(topicAggregateId, unitOfWork);
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