package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    Optional<Question> findTopByOrderByVersionDesc();

    default Optional<Question> findLatestQuestion() {
        return findTopByOrderByVersionDesc();
    }
}
