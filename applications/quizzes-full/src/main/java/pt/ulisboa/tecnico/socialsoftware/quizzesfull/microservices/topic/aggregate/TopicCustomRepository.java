package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate;

import java.util.List;

public interface TopicCustomRepository {
    List<Integer> findTopicIdsByCourseId(Integer courseAggregateId);
}
