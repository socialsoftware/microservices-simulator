package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;

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
    
}
