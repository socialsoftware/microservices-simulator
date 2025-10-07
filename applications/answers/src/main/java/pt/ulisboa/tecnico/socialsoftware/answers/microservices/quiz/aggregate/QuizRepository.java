package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface QuizRepository extends JpaRepository<Quiz, Integer> {

}