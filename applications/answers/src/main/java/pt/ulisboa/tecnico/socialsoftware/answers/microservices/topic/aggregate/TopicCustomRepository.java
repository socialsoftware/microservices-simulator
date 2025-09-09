package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface TopicCustomRepository {
    Optional<Integer> findTopicIdByName(String topicName);
}