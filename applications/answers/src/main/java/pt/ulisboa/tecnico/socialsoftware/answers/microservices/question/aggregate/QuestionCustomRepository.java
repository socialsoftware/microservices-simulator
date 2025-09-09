package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface QuestionCustomRepository {
    Optional<Integer> findQuestionIdByTitle(String questionTitle);
}