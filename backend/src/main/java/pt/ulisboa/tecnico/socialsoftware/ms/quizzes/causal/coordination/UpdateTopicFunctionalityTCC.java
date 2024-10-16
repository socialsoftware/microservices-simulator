package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;

public class UpdateTopicFunctionalityTCC extends WorkflowFunctionality {
    private Topic oldTopic;
    private final TopicService topicService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public UpdateTopicFunctionalityTCC(TopicService topicService, CausalUnitOfWorkService unitOfWorkService,  
                            TopicDto topicDto, TopicFactory topicFactory, CausalUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicDto, topicFactory, unitOfWork);
    }

    public void buildWorkflow(TopicDto topicDto, TopicFactory topicFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            topicService.updateTopic(topicDto, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public Topic getOldTopic() {
        return oldTopic;
    }

    public void setOldTopic(Topic oldTopic) {
        this.oldTopic = oldTopic;
    }
}