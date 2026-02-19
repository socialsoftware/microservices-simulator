package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    Optional<Question> findTopByOrderByVersionDesc();

    default Optional<Question> findLatestQuestion() {
        return findTopByOrderByVersionDesc();
    }

    @Query("SELECT q.aggregateId FROM Question q")
    Set<Integer> findAllAggregateIds();
}
