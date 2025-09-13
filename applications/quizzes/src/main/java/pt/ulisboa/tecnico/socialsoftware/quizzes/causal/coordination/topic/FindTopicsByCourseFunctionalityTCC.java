package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.FindTopicsByCourseIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

import java.util.List;
import java.util.stream.Collectors;

public class FindTopicsByCourseFunctionalityTCC extends WorkflowFunctionality {
    private List<TopicDto> topics;
    @SuppressWarnings("unused")
    private final TopicService topicService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindTopicsByCourseFunctionalityTCC(TopicService topicService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // this.topics = topicService.findTopicsByCourseId(courseAggregateId, unitOfWork);
            FindTopicsByCourseIdCommand FindTopicsByCourseIdCommand = new FindTopicsByCourseIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), courseAggregateId);
            Object result = commandGateway.send(FindTopicsByCourseIdCommand);
            List<?> list = (List<?>) result;
            this.topics = list.stream().map(o -> (TopicDto) o).collect(Collectors.toList());
        });
    
        workflow.addStep(step);
    }
    

    public List<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDto> topics) {
        this.topics = topics;
    }
}