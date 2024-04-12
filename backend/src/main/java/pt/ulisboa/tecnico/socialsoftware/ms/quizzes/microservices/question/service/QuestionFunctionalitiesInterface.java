package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;

public interface QuestionFunctionalitiesInterface {
    QuestionDto findQuestionByAggregateId(Integer aggregateId);
    List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId);
    QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws Exception;
    void updateQuestion(QuestionDto questionDto) throws Exception;
    void removeQuestion(Integer questionAggregateId) throws Exception;
    void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) throws Exception;
}
