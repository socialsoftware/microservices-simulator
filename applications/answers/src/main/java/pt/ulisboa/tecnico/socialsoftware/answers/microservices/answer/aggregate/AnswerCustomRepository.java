package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.Optional;

public interface AnswerCustomRepository {
    Optional<Integer> findAnswerIdByQuizIdAndUserId(Integer quizAggregateId, Integer studentAggregateId);
}