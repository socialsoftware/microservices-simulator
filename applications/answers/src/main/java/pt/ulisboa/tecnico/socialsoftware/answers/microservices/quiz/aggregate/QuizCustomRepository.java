package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.util.Set;

public interface QuizCustomRepository {
    Set<Integer> findAllQuizIdsByExecution(Integer executionAggregateId);
}