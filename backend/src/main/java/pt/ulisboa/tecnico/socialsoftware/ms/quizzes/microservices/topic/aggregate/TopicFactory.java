package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate;

public interface TopicFactory {
    Topic createTopic(Integer aggregateId, String name, TopicCourse topicCourse);
    Topic createTopicFromExisting(Topic existingTopic);
    TopicDto createTopicDto(Topic topic);
}
