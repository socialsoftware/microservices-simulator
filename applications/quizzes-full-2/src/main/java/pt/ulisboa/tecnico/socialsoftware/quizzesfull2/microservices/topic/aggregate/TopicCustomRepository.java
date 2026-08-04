package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate;

import java.util.Set;

public interface TopicCustomRepository {
    Set<Integer> findTopicIdsByCourse(Integer courseAggregateId);
}
