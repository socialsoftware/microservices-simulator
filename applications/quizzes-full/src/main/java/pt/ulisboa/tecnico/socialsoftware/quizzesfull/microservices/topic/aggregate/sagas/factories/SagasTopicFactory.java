package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.SagaTopic;

@Service
@Profile("sagas")
public class SagasTopicFactory implements TopicFactory {

    @Override
    public SagaTopic createTopic(Integer aggregateId, String name, TopicCourse topicCourse) {
        return new SagaTopic(aggregateId, name, topicCourse);
    }

    @Override
    public SagaTopic createTopicCopy(Topic existing) {
        return new SagaTopic((SagaTopic) existing);
    }

    @Override
    public TopicDto createTopicDto(Topic topic) {
        return new TopicDto(topic);
    }
}
