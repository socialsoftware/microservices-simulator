package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;

public class DeleteTopicFunctionalityTCC extends WorkflowFunctionality {
    private CausalTopic topic;
    private final TopicService topicService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public DeleteTopicFunctionalityTCC(TopicService topicService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer topicAggregateId, CausalUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            topicService.deleteTopic(topicAggregateId, unitOfWork);
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