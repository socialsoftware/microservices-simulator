package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate;

import java.util.Optional;

public interface QuizAnswerCustomRepository {
    Optional<QuizAnswer> findByQuizAggregateIdAndUserAggregateId(Integer quizAggregateId, Integer userAggregateId);
}
