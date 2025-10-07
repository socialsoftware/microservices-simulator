package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import java.util.Optional;

public interface TopicCustomRepository {
    Optional<Integer> findTopicIdByName(String topicName);
}