package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import java.time.LocalDateTime;

public interface QuizAnswerFactory {
    QuizAnswer createQuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion,
                                Integer userAggregateId, String userName, Long userVersion,
                                Integer executionAggregateId, Long executionVersion,
                                LocalDateTime creationDate, LocalDateTime answerDate);

    QuizAnswer createQuizAnswerCopy(QuizAnswer existing);

    QuizAnswerDto createQuizAnswerDto(QuizAnswer quizAnswer);
}
