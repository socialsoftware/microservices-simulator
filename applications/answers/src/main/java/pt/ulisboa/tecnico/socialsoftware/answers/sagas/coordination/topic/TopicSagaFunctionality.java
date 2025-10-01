package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;

@Component
public class TopicSagaFunctionality extends WorkflowFunctionality {
private final TopicService topicService;
private final SagaUnitOfWorkService unitOfWorkService;

public TopicSagaFunctionality(TopicService topicService, SagaUnitOfWorkService
unitOfWorkService) {
this.topicService = topicService;
this.unitOfWorkService = unitOfWorkService;
}

    public Object createTopic(String name, Object course, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for createTopic
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getTopicById(Integer topicId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getTopicById
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getAllTopics(SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAllTopics
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getTopicsByCourse(Integer courseId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getTopicsByCourse
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object updateTopic(Integer topicId, String name, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for updateTopic
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object deleteTopic(Integer topicId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for deleteTopic
        // This method should orchestrate the saga workflow
        return null;
    }
}