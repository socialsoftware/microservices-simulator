package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

public class GetTopicByIdFunctionalityTCC extends WorkflowFunctionality {
    private TopicDto topicDto;
    private final TopicService topicService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetTopicByIdFunctionalityTCC(TopicService topicService, CausalUnitOfWorkService unitOfWorkService,  
                                Integer topicAggregateId, CausalUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.topicDto = topicService.getTopicById(topicAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public void setTopicDto(TopicDto topicDto) {
        this.topicDto = topicDto;
    }

    public TopicDto getTopicDto() {
        return this.topicDto;
    }

}
