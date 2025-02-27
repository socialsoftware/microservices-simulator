package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTopicDto;

@Service
@Profile("sagas")
public class SagasTopicFactory implements TopicFactory {

    @Override
    public Topic createTopic(Integer aggregateId, String name, TopicCourse topicCourse) {
        return new SagaTopic(aggregateId, name, topicCourse);
    }

    @Override
    public Topic createTopicFromExisting(Topic existingTopic) {
        return new SagaTopic((SagaTopic) existingTopic);
    }
    
    @Override
    public TopicDto createTopicDto(Topic topic) {
        return new SagaTopicDto(topic);
    }
}
