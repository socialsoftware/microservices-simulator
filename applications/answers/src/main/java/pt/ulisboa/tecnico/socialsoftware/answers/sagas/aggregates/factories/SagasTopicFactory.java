package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;

@Service
@Profile("sagas")
public class SagasTopicFactory extends TopicFactory {
@Override
public Topic createTopic(Integer aggregateId, TopicDto topicDto) {
return new SagaTopic(topicDto);
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