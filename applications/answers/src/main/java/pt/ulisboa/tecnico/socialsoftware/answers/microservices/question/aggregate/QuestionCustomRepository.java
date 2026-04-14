package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.Set;

public interface QuestionCustomRepository {
    Set<Integer> findQuestionIdsByTitlePattern(String titlePattern);
}