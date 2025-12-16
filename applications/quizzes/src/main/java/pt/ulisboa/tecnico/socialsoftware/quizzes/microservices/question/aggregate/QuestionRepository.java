package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    @Query(value = "select q1 from Question q1 where q1.version = (select max(q2.version) from Question q2)")
    Optional<Question> findLatestQuestion();
}
