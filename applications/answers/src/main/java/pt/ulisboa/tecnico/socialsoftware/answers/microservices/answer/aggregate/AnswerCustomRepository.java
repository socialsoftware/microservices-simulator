package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface AnswerCustomRepository {
    Optional<Integer> findQuizAnswerIdByQuizIdAndUserId(Integer quizAggregateId, Integer studentAggregateId);
}