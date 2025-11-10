package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    Optional<Integer> findQuestionIdByTitle(String questionTitle);
}