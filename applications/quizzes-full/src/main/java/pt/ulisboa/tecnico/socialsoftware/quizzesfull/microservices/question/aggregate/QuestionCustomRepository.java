package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate;

import java.util.List;

public interface QuestionCustomRepository {
    List<Integer> findQuestionIdsByCourseId(Integer courseAggregateId);
}
