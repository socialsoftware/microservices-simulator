package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;

public interface TopicFunctionalitiesInterface {
    List<TopicDto> findTopicsByCourseAggregateId(Integer courseAggregateId);
    TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws Exception;
    void updateTopic(TopicDto topicDto) throws Exception;
    void deleteTopic(Integer topicAggregateId) throws Exception;
}
