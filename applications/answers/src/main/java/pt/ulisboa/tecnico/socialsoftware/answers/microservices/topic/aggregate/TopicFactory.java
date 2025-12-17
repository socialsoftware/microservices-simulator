package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

public interface TopicFactory {
    Topic createTopic(Integer aggregateId, TopicCourse course, TopicDto topicDto);
    Topic createTopicFromExisting(Topic existingTopic);
    TopicDto createTopicDto(Topic topic);
}
