package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    @Query(value = "select q.aggregateId from Question q where q.title LIKE :titlePattern AND q.state != 'DELETED'")
    Set<Integer> findQuestionIdsByTitlePattern(String titlePattern);
}