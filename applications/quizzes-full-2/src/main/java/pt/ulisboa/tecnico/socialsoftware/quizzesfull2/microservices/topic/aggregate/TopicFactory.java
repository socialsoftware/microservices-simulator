package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate;

public interface TopicFactory {
    Topic createTopic(Integer aggregateId, String name, Integer courseAggregateId);

    Topic createTopicCopy(Topic existing);

    TopicDto createTopicDto(Topic topic);
}
