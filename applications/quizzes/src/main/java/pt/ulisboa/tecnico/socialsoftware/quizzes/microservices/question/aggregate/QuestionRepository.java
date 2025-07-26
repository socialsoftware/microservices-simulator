package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
}
