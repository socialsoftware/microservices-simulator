package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
}
