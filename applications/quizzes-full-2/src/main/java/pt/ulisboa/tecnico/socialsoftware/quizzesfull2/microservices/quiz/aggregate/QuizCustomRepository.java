package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

import java.util.Set;

public interface QuizCustomRepository {
    Set<Integer> findQuizIdsByExecution(Integer executionAggregateId);
}
