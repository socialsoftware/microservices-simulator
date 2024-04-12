package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;

public interface QuizFunctionalitiesInterface {
    QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) throws Exception;
    QuizDto findQuiz(Integer quizAggregateId);
    List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId);
    QuizDto updateQuiz(QuizDto quizDto) throws Exception;
}
