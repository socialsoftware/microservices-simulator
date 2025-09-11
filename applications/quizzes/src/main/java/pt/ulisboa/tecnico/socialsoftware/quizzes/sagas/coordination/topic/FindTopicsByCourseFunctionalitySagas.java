package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.FindTopicsByCourseIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

import java.util.List;

public class FindTopicsByCourseFunctionalitySagas extends WorkflowFunctionality {
    private List<TopicDto> topics;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindTopicsByCourseFunctionalitySagas(TopicService topicService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep findTopicsStep = new SagaSyncStep("findTopicsStep", () -> {
//            List<TopicDto> topics = topicService.findTopicsByCourseId(courseAggregateId, unitOfWork);
            FindTopicsByCourseIdCommand findTopicsByCourseIdCommand = new FindTopicsByCourseIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), courseAggregateId);
            List<TopicDto> topics = (List<TopicDto>) commandGateway.send(findTopicsByCourseIdCommand);
            this.setTopics(topics);
        });

        workflow.addStep(findTopicsStep);
    }
    

    public List<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDto> topics) {
        this.topics = topics;
    }
}