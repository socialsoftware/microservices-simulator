package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;

@Service
@Profile("sagas")
public class SagasTopicFactory implements TopicFactory {
    @Override
    public Topic createTopic(Integer aggregateId, TopicCourse course, TopicDto topicDto) {
        return new SagaTopic(aggregateId, course, topicDto);
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