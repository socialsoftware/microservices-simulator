package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import java.util.Set;

public interface QuestionCustomRepository {
    Set<Integer> findQuestionIdsByCourse(Integer courseAggregateId);
}
