package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.Optional;

public interface AnswerCustomRepository {
    Optional<Integer> findAnswerIdByQuizQuizAggregateIdAndUserUserAggregateId(Integer quizAggregateId, Integer userAggregateId);
}