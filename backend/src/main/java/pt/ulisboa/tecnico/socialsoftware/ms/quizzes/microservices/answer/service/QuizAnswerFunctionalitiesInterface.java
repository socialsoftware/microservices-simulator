package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;

public interface QuizAnswerFunctionalitiesInterface {
    void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto questionAnswerDto) throws Exception;
}
