package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate;

public interface TopicFactory {
    Topic createTopic(Integer aggregateId, String name, TopicCourse topicCourse);
    Topic createTopicCopy(Topic existing);
    TopicDto createTopicDto(Topic topic);
}
