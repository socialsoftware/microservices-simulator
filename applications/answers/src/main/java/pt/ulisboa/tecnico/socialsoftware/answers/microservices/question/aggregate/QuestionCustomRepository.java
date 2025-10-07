package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.Optional;

public interface QuestionCustomRepository {
    Optional<Integer> findQuestionIdByTitle(String questionTitle);
}