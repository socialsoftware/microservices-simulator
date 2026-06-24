package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate;

public interface TopicFactory {
    Topic createTopic(Integer aggregateId, TopicDto topicDto);

    Topic createTopicFromExisting(Topic existing);

    TopicDto createTopicDto(Topic topic);
}
