package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate;

import java.util.Optional;

public interface QuizAnswerCustomRepository {
    Optional<Integer> findQuizAnswerIdByQuizIdAndUserId(Integer quizAggregateId, Integer studentAggregateId);
}
