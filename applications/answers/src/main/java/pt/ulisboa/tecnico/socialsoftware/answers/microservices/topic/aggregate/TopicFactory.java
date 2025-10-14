package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

public interface TopicFactory {
    Topic createTopic(Integer aggregateId,  Dto);
    Topic createTopicFromExisting(Topic existingTopic);
     createTopicDto(Topic );
}
